#include <tusb.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <functional>
#include <map>
#include <memory>
#include <random>
#include <optional>
#include <string>

constexpr uint32_t CUSTOM_DEVICE_ID = 0;
constexpr bool     SOFTWARE_RESET_ON_DISCONNECT = false;
constexpr bool     ENABLE_DEBUG_LOG = false;

/* PINS */

constexpr uint8_t PIN_SPEAKER = 8;
constexpr uint8_t PIN_TACT_SWITCH = 9;
constexpr uint8_t PIN_LED_GREEN = 12;
constexpr uint8_t PIN_LED_BLUE = 11;
constexpr uint8_t PIN_LED_RED = 10;
constexpr uint8_t PIN_LIGHT_SENSOR = 28;

/* DATA */

enum class DataTypes : uint16_t {
  CommandDataGetImmediate = 0x0090,
  CommandDataGetLoopOff = 0x0092,
  CommandDataGetLoopOn = 0x0093,
  CommandDataSet = 0x00e0,

  ResponseDataSend = 0x00f0,
};

/* FIFO COMMANDS */

constexpr uint32_t FIFO_NO_TONE = 0xcafe0000;
constexpr uint32_t FIFO_TONE = 0xcafe000a;
constexpr uint32_t FIFO_RGB_LED = 0xcafe000b;
constexpr uint32_t FIFO_LED_BUILTIN = 0xcafe000c;
constexpr uint32_t FIFO_WAVEFORM = 0xcafe000d;

/* SENSORS */

constexpr size_t ANALOG_READINGS = 24;

#pragma region Dynamic Tone Implementation

#if !PICO_NO_HARDWARE
#include "hardware/pio.h"
#endif

#include <pico/rand.h>
#include <pico/time.h>

namespace dynamic_tone {

#pragma region tone_dynamic.pio.h

// ------------ //
// tone_dynamic //
// ------------ //

#define tone_dynamic_wrap_target 0
#define tone_dynamic_wrap 10
#define tone_dynamic_pio_version 0

static const uint16_t tone_dynamic_program_instructions[] = {
            //     .wrap_target
    0x8080, //  0: pull   noblock
    0xa047, //  1: mov    y, osr
    0x6030, //  2: out    x, 16
    0xa0c7, //  3: mov    isr, osr
    0xe001, //  4: set    pins, 1
    0x0045, //  5: jmp    x--, 5
    0xe000, //  6: set    pins, 0
    0xa026, //  7: mov    x, isr
    0x0048, //  8: jmp    x--, 8
    0xa0e2, //  9: mov    osr, y
    0xa027, // 10: mov    x, osr
            //     .wrap
};

#if !PICO_NO_HARDWARE
static const struct pio_program tone_dynamic_program = {
    .instructions = tone_dynamic_program_instructions,
    .length = 11,
    .origin = -1,
    .pio_version = tone_dynamic_pio_version,
#if PICO_PIO_VERSION > 0
    .used_gpio_ranges = 0x0
#endif
};

static inline pio_sm_config tone_dynamic_program_get_default_config(uint offset) {
    pio_sm_config c = pio_get_default_sm_config();
    sm_config_set_wrap(&c, offset + tone_dynamic_wrap_target, offset + tone_dynamic_wrap);
    return c;
}

static inline void tone_dynamic_program_init(PIO pio, uint sm, uint offset, uint pin) {
  pio_gpio_init(pio, pin);
  pio_sm_set_consecutive_pindirs(pio, sm, pin, 1, true);
  pio_sm_config c = tone_dynamic_program_get_default_config(offset);
  sm_config_set_set_pins(&c, pin, 1);
  pio_sm_init(pio, sm, offset, &c);
}

#endif

#pragma endregion

#pragma region ToneDynamic.h, cpp

void toneDynamic(uint8_t pin, float frequency, uint32_t duration = 0, float dutyCycle = 0.5);

void noToneDynamic(uint8_t pin);

void toneDynamicUpdate(uint8_t pin, float frequency, float dutyCycle = 0.5);

namespace {

// Fixed: pull, mov, out, mov, set, set, mov, mov (8 instructions)
//        + 1 cycle at jmp end = 9 cycles
// Variable: high_loop (X+1 cycles) + low_loop (X + 1 cycles)
constexpr int PIO_INSTRUCTION_OVERHEAD = 9;
constexpr int PIO_MIN_PHASE_CYCLES = 1;

constexpr uint32_t MAX_SINGLE_PHASE = 0xFFFF;
constexpr uint32_t MAX_REPRESENTABLE_CYCLES =
    PIO_INSTRUCTION_OVERHEAD + MAX_SINGLE_PHASE + PIO_MIN_PHASE_CYCLES;

struct Tone {
  pin_size_t pin;
  PIO pio;
  int sm;
  int off;
  float current_clkdiv;
  alarm_id_t alarm;
};

struct FrequencyConfig {
  uint32_t packed_value;
  float clkdiv;
};

auto_init_mutex(_toneMutex);

PIOProgram _toneDynamicPgm(&tone_dynamic_program);
std::map<pin_size_t, Tone *> _toneMap;

bool pio_sm_get_enabled(PIO pio, uint sm) {
  check_pio_param(pio);
  check_sm_param(sm);

  return (pio->ctrl & ~(1u << sm)) & (1u << sm);
}

int64_t _stopTonePIO(const alarm_id_t id, void *user_data) {
  (void)id;

  const auto tone = static_cast<Tone *>(user_data);

  tone->alarm = 0;

  digitalWrite(tone->pin, LOW);
  pinMode(tone->pin, OUTPUT);

  pio_sm_set_enabled(tone->pio, tone->sm, false);

  return 0;
}

std::optional<FrequencyConfig> make_packed_value(const float frequency,
                                                 float duty_cycle) {
  if (frequency <= 0)
    return std::nullopt;

  duty_cycle = std::clamp(duty_cycle, 0.f, 1.f);

  const uint32_t sys_clk = RP2040::f_cpu();

  const uint64_t required_cycles64 =
      (sys_clk + frequency / 2) / frequency; // 四捨五入

  const auto required_cycles = static_cast<uint32_t>(required_cycles64);
  float clkdiv = 1;

  if (required_cycles > MAX_REPRESENTABLE_CYCLES) {
    clkdiv = static_cast<float>(required_cycles) / MAX_REPRESENTABLE_CYCLES;

    if (clkdiv > 65536) {
      // Frequency too low
      return std::nullopt;
    }
  }

  const auto effective_clk = static_cast<uint64_t>(sys_clk / clkdiv);

  const uint64_t total_cycles64 =
      (effective_clk + frequency / 2) / frequency; // 四捨五入

  const auto total_cycles = static_cast<uint32_t>(total_cycles64);

  if (total_cycles <= PIO_INSTRUCTION_OVERHEAD) {
    // Frequency too high
    return std::nullopt;
  }

  const uint32_t variable_cycles =
      total_cycles - PIO_INSTRUCTION_OVERHEAD; // = (high_phase + low_phase)

  constexpr uint32_t min_phase = PIO_MIN_PHASE_CYCLES;

  if (variable_cycles < min_phase * 2) {
    // Insufficient cycles for stable operation
    return std::nullopt;
  }

  const uint32_t high_phase =
      std::clamp(static_cast<uint32_t>(lround(variable_cycles * duty_cycle)),
                 min_phase, variable_cycles - min_phase);

  const uint32_t low_phase = variable_cycles - high_phase;

  const uint32_t high_val = high_phase - PIO_MIN_PHASE_CYCLES;
  const uint32_t low_val = low_phase - PIO_MIN_PHASE_CYCLES;

  if (high_val > 0xFFFF || low_val > 0xFFFF) {
    // Should not happen due to previous checks
    return std::nullopt;
  }

  return FrequencyConfig{
      .packed_value = (static_cast<uint32_t>(high_val) << 16) |
                      static_cast<uint32_t>(low_val),
      .clkdiv = clkdiv,
  };
}

} // namespace

void toneDynamic(const uint8_t pin, const float frequency,
                 const uint32_t duration, const float dutyCycle) {
  if (pin >= __GPIOCNT)
    return;

  if (frequency <= 0) {
    noToneDynamic(pin);

    return;
  }

  CoreMutex m(&_toneMutex);

  if (!m)
    return;

  const auto freq_config = make_packed_value(frequency, dutyCycle);

  if (!freq_config)
    return;

  auto entry = _toneMap.find(pin);
  Tone *tone = nullptr;

  if (entry == _toneMap.end()) {
    tone = new Tone{.pin = pin, .current_clkdiv = 1, .alarm = 0};

    pinMode(pin, OUTPUT);

    if (!_toneDynamicPgm.prepare(&tone->pio, &tone->sm, &tone->off, pin, 1)) {
      delete tone;

      return;
    }
  } else {
    tone = entry->second;

    if (tone->alarm) {
      cancel_alarm(tone->alarm);

      tone->alarm = 0;
    }
  }

  if (!pio_sm_get_enabled(tone->pio, tone->sm)) {
    tone_dynamic_program_init(tone->pio, tone->sm, tone->off, pin);

    tone->current_clkdiv = freq_config->clkdiv;

    pio_sm_set_clkdiv(tone->pio, tone->sm, freq_config->clkdiv);
  } else if (tone->current_clkdiv != freq_config->clkdiv) {
    tone->current_clkdiv = freq_config->clkdiv;

    pio_sm_set_clkdiv(tone->pio, tone->sm, freq_config->clkdiv);
  }

  pio_sm_clear_fifos(tone->pio, tone->sm);
  pio_sm_put(tone->pio, tone->sm, freq_config->packed_value);
  pio_sm_set_enabled(tone->pio, tone->sm, true);

  _toneMap[pin] = tone;

  if (duration) {
    const auto ret = add_alarm_in_ms(duration, _stopTonePIO, tone, true);

    if (ret > 0) {
      tone->alarm = ret;
    }
  }
}

void noToneDynamic(const uint8_t pin) {
  CoreMutex m(&_toneMutex);

  if ((pin > __GPIOCNT) || !m)
    return;

  auto entry = _toneMap.find(pin);

  if (entry == _toneMap.end())
    return;

  if (entry->second->alarm) {
    cancel_alarm(entry->second->alarm);

    entry->second->alarm = 0;
  }

  pio_sm_set_enabled(entry->second->pio, entry->second->sm, false);
  pio_sm_unclaim(entry->second->pio, entry->second->sm);

  delete entry->second;

  _toneMap.erase(entry);

  pinMode(pin, OUTPUT);
  digitalWrite(pin, LOW);
}

void toneDynamicUpdate(const uint8_t pin, const float frequency,
                       const float dutyCycle) {
  if (pin >= __GPIOCNT || frequency <= 0)
    return;

  CoreMutex m(&_toneMutex);

  if (!m)
    return;

  const auto entry = _toneMap.find(pin);

  if (entry == _toneMap.end())
    return;

  const auto tone = entry->second;

  const auto freq_config = make_packed_value(frequency, dutyCycle);

  if (!freq_config)
    return;

  if (tone->current_clkdiv != freq_config->clkdiv) {
    tone->current_clkdiv = freq_config->clkdiv;

    pio_sm_set_clkdiv(tone->pio, tone->sm, freq_config->clkdiv);
  }

  pio_sm_clear_fifos(tone->pio, tone->sm);
  pio_sm_put(tone->pio, tone->sm, freq_config->packed_value);
}

#pragma endregion

#pragma region Waveform.h

class Waveform {
public:
  using value_type = uint8_t;
  using table_type = std::array<value_type, 1 << 8>;

  virtual ~Waveform() = default;

  [[nodiscard]] virtual uint16_t get_size() const = 0;

  [[nodiscard]] virtual value_type operator[](std::size_t idx) const = 0;
};

class DataWaveform final : public Waveform {
public:
  template <std::size_t N>
  explicit DataWaveform(const std::array<value_type, N> &tbl) : size(N) {
    static_assert((N & (N - 1)) == 0, "Size must be a power of 2");
    static_assert(N <= 1 << 8, "Size exceeds maximum table size");

    std::copy_n(tbl.begin(), N, table.begin());
  }

  [[nodiscard]] uint16_t get_size() const override { return size; }

  [[nodiscard]] value_type operator[](const std::size_t idx) const override {
    return table[idx & (size - 1)];
  }

private:
  uint16_t size;
  table_type table{};
};

class NoiseWaveform final : public Waveform {
public:
  NoiseWaveform() : rng(get_rand_32()) {}

  [[nodiscard]] uint16_t get_size() const override { return 1; }

  [[nodiscard]] value_type operator[](std::size_t idx) const override {
    return static_cast<value_type>(rng() & 0xFF);
  }

private:
  mutable std::mt19937 rng;
};

// Generation JavaScript:
// const waveLUT = (N, f) => [...Array(N).keys()].map(i => i / N).map(f).map(v
// => Math.round(127.5 + v * 127.5))

const DataWaveform SQUARE_WAVEFORM{
    std::array<Waveform::value_type, 2>{{255, 0}}};

const DataWaveform SQUARE_25_WAVEFORM{
  std::array<Waveform::value_type, 4>{{255, 0, 0, 0}}};

const DataWaveform SQUARE_12_WAVEFORM{
    std::array<Waveform::value_type, 8>{{255, 0, 0, 0, 0, 0, 0, 0}}};

// waveLUT(16, t => t < 0.25 ? 4 * t : t < 0.75 ? -4 * t + 2 : 4 * t - 4)
const DataWaveform TRIANGLE_WAVEFORM{std::array<Waveform::value_type, 16>{
    {128, 159, 191, 223, 255, 223, 191, 159, 128, 96, 64, 32, 0, 32, 64, 96}}};

// waveLUT(16, t => t => t < 0.5 ? 2 * t : 2 * t - 2)
const DataWaveform SAW_WAVEFORM{std::array<Waveform::value_type, 16>{
    {128, 143, 159, 175, 191, 207, 223, 239, 0, 16, 32, 48, 64, 80, 96, 112}}};

// waveLUT(16, t => Math.sin(2 * Math.PI * t))
const DataWaveform SINE_WAVEFORM{std::array<Waveform::value_type, 16>{
    {128, 176, 218, 245, 255, 245, 218, 176, 128, 79, 37, 10, 0, 10, 37, 79}}};

const NoiseWaveform NOISE_WAVEFORM{};

#pragma endregion

#pragma region Speaker.h, cpp

class Speaker {
public:
  explicit Speaker(uint16_t pin, bool use_core1 = false, float freq = 440, float volume = 1);

  Speaker(const Speaker &) = delete;
  Speaker &operator=(const Speaker &) = delete;

  Speaker(Speaker &&) = delete;
  Speaker &operator=(Speaker &&) = delete;

  [[nodiscard]] float get_frequency() const { return audible_freq; }

  [[nodiscard]] float get_volume() const { return volume; }

  [[nodiscard]] bool playing() const { return is_playing; }

  void set_frequency(float freq);

  void set_volume(float vol);

  void set_waveform(const Waveform &wf);

  void play(uint32_t duration = 0);

  void stop();

private:
  static constexpr float carrier_freq = 1000 * 1000; // 1MHz carrier

  uint16_t pin;

  const Waveform *next_waveform = nullptr;
  const Waveform *waveform = &SQUARE_WAVEFORM;

  float audible_freq = 440;
  float volume = 1;
  float duty_scale = 1;
  uint32_t lut_period_us = 0;

  bool is_playing = false;
  uint32_t playback_end_ms = 0;
  bool freq_changed = false;
  uint16_t waveform_index = 0;

  alarm_pool_t *alarm_pool;
  repeating_timer timer{};

  void refresh_lut_period();

  void repeating_timer_cb(repeating_timer *t);
};

namespace {

constexpr float V_PEAK = 3.3f; // V

float calculate_duty_scale_for_volume_basic(float volume) {
  volume = std::clamp(volume, 0.0f, 1.0f);

  if (volume <= 0.01f)
    return 0.0f;

  const auto attenuation_db = 20.0f * std::log10(volume);
  const auto linear_ratio = std::pow(10.0f, attenuation_db / 20.0f);

  const auto v_rms_max = V_PEAK * std::sqrt(0.5f);
  const auto v_rms_target = v_rms_max * linear_ratio;
  const auto duty_cycle = (v_rms_target * v_rms_target) / (V_PEAK * V_PEAK);

  return duty_cycle * 2; // Scale to [0..1]
}

} // namespace

Speaker::Speaker(const uint16_t pin, const bool use_core1, const float freq,
                 const float volume)
    : pin(pin), audible_freq(freq) {
  if (use_core1) {
    alarm_pool = alarm_pool_create(1, 4);
  } else {
    alarm_pool = alarm_pool_get_default();
  }

  refresh_lut_period();
  set_volume(volume);
}

void Speaker::set_frequency(const float freq) {
  if (audible_freq == freq || freq <= 0)
    return;

  audible_freq = freq;

  refresh_lut_period();

  if (!is_playing)
    return;

  freq_changed = true;
}

void Speaker::set_volume(const float vol) {
  if (volume == vol)
    return;

  volume = std::clamp(vol, 0.f, 1.f);
  duty_scale = calculate_duty_scale_for_volume_basic(volume);
}

void Speaker::set_waveform(const Waveform &wf) {
  if (&wf == next_waveform)
    return;

  if (!is_playing) {
    waveform = &wf;

    refresh_lut_period();

    return;
  }

  next_waveform = &wf;
}

void Speaker::play(const uint32_t duration) {
  if (is_playing) {
    if (duration) {
      playback_end_ms = to_ms_since_boot(get_absolute_time()) + duration;
    } else {
      playback_end_ms = 0;
    }

    return;
  }

  toneDynamic(pin, carrier_freq, 0, 0);

  is_playing = true;

  alarm_pool_add_repeating_timer_us(
      alarm_pool, -static_cast<int64_t>(lut_period_us),
      [](repeating_timer *t) {
        static_cast<Speaker *>(t->user_data)->repeating_timer_cb(t);

        return true;
      },
      this, &timer);

  if (duration > 0) {
    playback_end_ms = to_ms_since_boot(get_absolute_time()) + duration;
  }
}

void Speaker::stop() {
  if (!is_playing)
    return;

  is_playing = false;
  playback_end_ms = 0;

  cancel_repeating_timer(&timer);

  noToneDynamic(pin);
}

void Speaker::refresh_lut_period() {
  lut_period_us =
      std::lround(1000 * 1000 / (waveform->get_size() * audible_freq));
}

void Speaker::repeating_timer_cb(repeating_timer *t) {
  if (!is_playing)
    return;

  if (playback_end_ms > 0 &&
      to_ms_since_boot(get_absolute_time()) >= playback_end_ms) {
    stop();

    return;
  }

  if (waveform_index == 0 && next_waveform) {
    waveform = next_waveform;
    next_waveform = nullptr;

    lut_period_us =
        std::lround(1000 * 1000 / (waveform->get_size() * audible_freq));

    freq_changed = true;
  }

  if (freq_changed) {
    t->delay_us = -static_cast<int64_t>(lut_period_us);

    freq_changed = false;
  }

  float duty_cycle =
      static_cast<float>(waveform->operator[](waveform_index)) / 255.f;
  duty_cycle = std::clamp(duty_cycle, 0.f, 1.f);

  waveform_index = (waveform_index + 1) & (waveform->get_size() - 1);

  toneDynamicUpdate(pin, carrier_freq, duty_cycle * duty_scale);
}

#pragma endregion

} // namespace dynamic_tone

#pragma endregion

namespace device_id {

#pragma region XXH32 Implementation

/*
 *  xxHash - Fast Hash algorithm
 *  Copyright (C) 2012-2020 Yann Collet
 *  Copyright (C) 2019-2020 Devin Hussey (easyaspi314)
 *
 *  BSD 2-Clause License (http://www.opensource.org/licenses/bsd-license.php)
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *
 *  * Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *  copyright notice, this list of conditions and the following disclaimer
 *  in the documentation and/or other materials provided with the
 *  distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  You can contact the author at :
 *  - xxHash homepage: http://www.xxhash.com
 *  - xxHash source repository : https://github.com/Cyan4973/xxHash */

/* This is a compact, 100% standalone reference XXH32 single-run implementation.
 * Instead of focusing on performance hacks, this focuses on cleanliness,
 * conformance, portability and simplicity.
 *
 * This file aims to be 100% compatible with C90/C++98, with the additional
 * requirement of stdint.h. No library functions are used. */

uint32_t XXH32(void const *const input, size_t const length,
               uint32_t const seed);

static uint32_t const PRIME32_1 =
    0x9E3779B1U; /* 0b10011110001101110111100110110001 */
static uint32_t const PRIME32_2 =
    0x85EBCA77U; /* 0b10000101111010111100101001110111 */
static uint32_t const PRIME32_3 =
    0xC2B2AE3DU; /* 0b11000010101100101010111000111101 */
static uint32_t const PRIME32_4 =
    0x27D4EB2FU; /* 0b00100111110101001110101100101111 */
static uint32_t const PRIME32_5 =
    0x165667B1U; /* 0b00010110010101100110011110110001 */

/* Rotates value left by amt. */
static uint32_t XXH_rotl32(uint32_t const value, uint32_t const amt) {
  return (value << (amt % 32)) | (value >> (32 - (amt % 32)));
}

/* Portably reads a 32-bit little endian integer from data at the given offset.
 */
static uint32_t XXH_read32(uint8_t const *const data, size_t const offset) {
  return (uint32_t)data[offset + 0] | ((uint32_t)data[offset + 1] << 8) |
         ((uint32_t)data[offset + 2] << 16) |
         ((uint32_t)data[offset + 3] << 24);
}

/* Mixes input into acc. */
static uint32_t XXH32_round(uint32_t acc, uint32_t const input) {
  acc += input * PRIME32_2;
  acc = XXH_rotl32(acc, 13);
  acc *= PRIME32_1;
  return acc;
}

/* Mixes all bits to finalize the hash. */
static uint32_t XXH32_avalanche(uint32_t hash) {
  hash ^= hash >> 15;
  hash *= PRIME32_2;
  hash ^= hash >> 13;
  hash *= PRIME32_3;
  hash ^= hash >> 16;
  return hash;
}

/* The XXH32 hash function.
 * input:   The data to hash.
 * length:  The length of input. It is undefined behavior to have length larger
 * than the capacity of input. seed:    A 32-bit value to seed the hash with.
 * returns: The 32-bit calculated hash value. */
uint32_t XXH32(void const *const input, size_t const length,
               uint32_t const seed) {
  uint8_t const *const data = (uint8_t const *)input;
  uint32_t hash;
  size_t remaining = length;
  size_t offset = 0;

  /* Don't dereference a null pointer. The reference implementation notably
   * doesn't check for this by default. */
  if (input == NULL) {
    return XXH32_avalanche(seed + PRIME32_5);
  }

  if (remaining >= 16) {
    /* Initialize our accumulators */
    uint32_t acc1 = seed + PRIME32_1 + PRIME32_2;
    uint32_t acc2 = seed + PRIME32_2;
    uint32_t acc3 = seed + 0;
    uint32_t acc4 = seed - PRIME32_1;

    while (remaining >= 16) {
      acc1 = XXH32_round(acc1, XXH_read32(data, offset));
      offset += 4;
      acc2 = XXH32_round(acc2, XXH_read32(data, offset));
      offset += 4;
      acc3 = XXH32_round(acc3, XXH_read32(data, offset));
      offset += 4;
      acc4 = XXH32_round(acc4, XXH_read32(data, offset));
      offset += 4;
      remaining -= 16;
    }

    hash = XXH_rotl32(acc1, 1) + XXH_rotl32(acc2, 7) + XXH_rotl32(acc3, 12) +
           XXH_rotl32(acc4, 18);
  } else {
    /* Not enough data for the main loop, put something in there instead. */
    hash = seed + PRIME32_5;
  }

  hash += (uint32_t)length;

  /* Process the remaining data. */
  while (remaining >= 4) {
    hash += XXH_read32(data, offset) * PRIME32_3;
    hash = XXH_rotl32(hash, 17);
    hash *= PRIME32_4;
    offset += 4;
    remaining -= 4;
  }

  while (remaining != 0) {
    hash += (uint32_t)data[offset] * PRIME32_5;
    hash = XXH_rotl32(hash, 11);
    hash *= PRIME32_1;
    --remaining;
    ++offset;
  }
  return XXH32_avalanche(hash);
}

#pragma endregion /* XXH32 Implementation */

uint32_t get() {
  if constexpr (CUSTOM_DEVICE_ID > 0) {
    return CUSTOM_DEVICE_ID;
  } else {
    static uint32_t id;
    static bool output_hash_got = false;

    if (!output_hash_got) {
      pico_unique_board_id_t raw_id;

      pico_get_unique_board_id(&raw_id);

      id = XXH32(raw_id.id, 8, 0);

      output_hash_got = true;
    }

    return id;
  }
}

} // namespace device_id

#pragma region Serial Communicator Implementation

// CRC16-CCITT Implementation
namespace crc16_ccitt {

constexpr uint16_t POLYNOMIAL = 0x1021;

std::array<uint16_t, 256> table{};
bool table_initialized = false;

void initialize_table() {
  for (uint16_t i = 0; i < 256; ++i) {
    auto crc = i;

    for (uint8_t j = 0; j < 8; ++j) {
      if (crc & 0x0001) {
        crc = (crc >> 1) ^ POLYNOMIAL;
      } else {
        crc >>= 1;
      }
    }

    table[i] = crc;
  }

  table_initialized = true;
}

uint16_t compute(const uint8_t *data, const size_t length,
                 const uint16_t initial_crc = 0xFFFF) {
  if (!table_initialized) {
    initialize_table();
  }

  uint16_t crc = initial_crc;

  for (size_t i = 0; i < length; ++i) {
    const auto byte = data[i];
    const uint8_t index = (crc ^ byte) & 0xFF;

    crc = (crc >> 8) ^ table[index];
  }

  return crc;
}

bool verify(const uint8_t *data, const size_t length,
            const uint16_t expected_crc, const uint16_t initial_crc = 0xFFFF) {
  return compute(data, length, initial_crc) == expected_crc;
}

} // namespace crc16_ccitt

namespace cobs {

size_t encode(const uint8_t *input, const size_t length, uint8_t *output) {
  if (length == 0) {
    output[0] = 1;

    return 1;
  }

  const auto *end = input + length;
  auto dst = output;
  auto code_ptr = dst++;

  uint8_t code = 1;

  while (input < end) {
    if (*input == 0) {
      *code_ptr = code;
      code_ptr = dst++;

      code = 1;

      ++input;
    } else {
      *dst++ = *input++;
      ++code;

      if (code == 0xFF) {
        *code_ptr = code;
        code_ptr = dst++;

        code = 1;
      }
    }
  }

  *code_ptr = code;

  return dst - output;
}

bool decode(const uint8_t *input, const size_t length, uint8_t *output,
            size_t &output_length) {
  if (length == 0) {
    output_length = 0;

    return true;
  }

  auto ptr = input;
  const auto *end = input + length;
  auto dst = output;

  while (ptr < end) {
    uint8_t code = *ptr++;

    if (code == 0 || ptr + (code - 1) > end) {
      // Invalid COBS data
      return false;
    }

    for (uint8_t i = 1; i < code; ++i) {
      *dst++ = *ptr++;
    }

    if (code < 0xFF && ptr < end) {
      *dst++ = 0;
    }
  }

  output_length = dst - output;

  return true;
}

} // namespace cobs

namespace bytes {

// Little-endian encoder
class Encoder {
public:
  explicit Encoder(std::vector<uint8_t> &buffer) : buffer(buffer) {}

  void push_byte(const uint8_t byte) const { buffer.push_back(byte); }

  void push_bytes(const uint8_t *data, const size_t length) const {
    buffer.insert(buffer.end(), data, data + length);
  }

  template <typename T> void push_number(const T number) const {
    static_assert(std::is_integral_v<T> || std::is_floating_point_v<T>);

    const auto ptr = reinterpret_cast<const uint8_t *>(&number);

    push_bytes(ptr, sizeof(T));
  }

  void push_bool(const bool value) const { push_byte(value ? 0xFF : 0); }

  void push_string(const std::string &str) const {
    push_bytes(reinterpret_cast<const uint8_t *>(str.data()), str.size());
  }

private:
  std::vector<uint8_t> &buffer;
};

// Consuming little-endian decoder
class Decoder {
public:
  explicit Decoder(const uint8_t *data, const size_t length)
      : data(data), length(length) {}

  [[nodiscard]] size_t remaining() const { return length - offset; }

  [[nodiscard]] uint8_t pop_byte() {
    assert(length > offset);

    const auto byte = data[offset++];

    return byte;
  }

  void pop_bytes(uint8_t *dst, const size_t len) {
    assert(remaining() >= len);

    std::copy_n(data + offset, len, dst);

    offset += len;
  }

  template <typename T> [[nodiscard]] T pop_number() {
    static_assert(std::is_integral_v<T> || std::is_floating_point_v<T>);

    assert(remaining() >= sizeof(T));

    T number;

    pop_bytes(reinterpret_cast<uint8_t *>(&number), sizeof(T));

    return number;
  }

  [[nodiscard]] bool pop_bool() { return pop_byte() != 0; }

  [[nodiscard]] std::string pop_string(const size_t len) {
    assert(remaining() >= len);

    std::string str(reinterpret_cast<const char *>(data + offset), len);

    offset += len;

    return str;
  }

private:
  const uint8_t *data;
  size_t offset = 0;
  size_t length;
};

} // namespace bytes

namespace packets {

// COBS max chunk size
constexpr size_t MAX_CHUNK_SIZE = 44; // 64 - 5 (cobs) - 14 (header+crc) - 1 (delimiter)
constexpr size_t MAX_FRAME_SIZE = 2048;

constexpr uint32_t RX_FRAME_TIMEOUT_MS = 2000;

struct ChunkHeader {
  uint16_t type;
  uint32_t frame_id;
  uint16_t total_chunks;
  uint16_t chunk_index;
  uint16_t payload_size;
  uint16_t crc16;
};

struct Frame {
  uint16_t type;
  uint32_t frame_id;
  uint16_t total_chunks;
  uint16_t received_chunks = 0;
  uint32_t last_update_ms;
  std::vector<std::vector<uint8_t>> chunks;
  bool invalid = false;

  Frame(const uint16_t type, const uint32_t frame_id,
        const uint16_t total_chunks)
      : type(type), frame_id(frame_id), total_chunks(total_chunks),
        last_update_ms(millis()), chunks(total_chunks) {}
};

struct Packet {
  const uint16_t type;
  const std::vector<uint8_t> payload;

  Packet(const uint16_t type, std::vector<uint8_t> &&payload)
      : type(type), payload(std::move(payload)) {}
};

class Socket {
public:
  static constexpr uint16_t TYPE_DEBUG_ECHO = 0xFFFF;

  virtual ~Socket() = default;

  virtual bool is_available() = 0;

  void send(const Packet &packet) {
    send_frame(packet.type, next_frame_id++, packet.payload.data(),
               packet.payload.size());
  }

  void send_debug(const std::string &str) {
    if constexpr (!ENABLE_DEBUG_LOG) return;

    std::vector<uint8_t> payload;

    const bytes::Encoder encoder{payload};

    encoder.push_string(str);

    send(Packet(TYPE_DEBUG_ECHO, std::move(payload)));
  }

  void update() {
    if (!is_available() && prev_availability) {
      rx_buffer.clear();

      prev_availability = false;

      on_unavailable();

      return;
    }

    if (!prev_availability) {
      prev_availability = true;

      on_available();
    }

    recv_raw_data(rx_buffer);

    if (!rx_buffer.empty()) {
      process_rx_buffer();
    }

    cleanup_stale_frames();
  }

private:
  std::vector<Frame> frame_table{};
  std::vector<uint8_t> rx_buffer;

  bool prev_availability = false;
  size_t next_frame_id = 0;

  virtual void on_unavailable() {}

  virtual void on_available() {}

  virtual void recv_raw_data(std::vector<uint8_t> &data) = 0;

  virtual void send_raw_data(const uint8_t *data, const size_t len) = 0;

  virtual void on_recv(Packet packet) = 0;

  std::optional<Packet> recv(const uint8_t *buffer, const size_t length) {
    if (length < 12) {
      send_debug("Packet too short");

      // Not enough data for even the smallest chunk
      // type(2) + frame_id(4) + total_chunks(2) + chunk_index(2) +
      // payload_size(2)
      return std::nullopt;
    }

    bytes::Decoder decoder(buffer, length);

    const auto type = decoder.pop_number<uint16_t>();
    const auto frame_id = decoder.pop_number<uint32_t>();
    const auto total_chunks = decoder.pop_number<uint16_t>();
    const auto chunk_index = decoder.pop_number<uint16_t>();
    const auto payload_size = decoder.pop_number<uint16_t>();

    if (payload_size != decoder.remaining() - 2) {
      send_debug("Invalid payload size");

      return std::nullopt;
    }

    if (payload_size > MAX_CHUNK_SIZE) {
      send_debug("Payload size too large");

      return std::nullopt;
    }

    auto data = std::vector<uint8_t>(payload_size);

    decoder.pop_bytes(data.data(), payload_size);

    const auto crc16 = decoder.pop_number<uint16_t>();

    if (!crc16_ccitt::verify(data.data(), payload_size, crc16)) {
      // CRC mismatch
      // Find existing entry and mark as invalid
      const auto frame = std::find_if(
          frame_table.begin(), frame_table.end(),
          [frame_id](const Frame &f) { return f.frame_id == frame_id; });

      if (frame != frame_table.end()) {
        frame->invalid = true;
      }

      send_debug("CRC mismatch");

      return std::nullopt;
    }

    auto *frame = get_or_create_frame(type, frame_id, total_chunks);

    if (frame == nullptr || frame->invalid) {
      // Invalid frame
      return std::nullopt;
    }

    if (frame->chunks.size() != total_chunks) {
      // Adjust chunks
      frame->chunks.resize(total_chunks);
    }

    if (chunk_index >= total_chunks || !frame->chunks[chunk_index].empty()) {
      send_debug("Invalid chunk index or duplicate chunk");

      // Invalid chunk index or duplicate chunk
      return std::nullopt;
    }

    frame->chunks[chunk_index] = std::move(data);
    frame->received_chunks++;
    frame->last_update_ms = millis();

    // If all chunks received, reassemble
    if (frame->received_chunks == frame->total_chunks) {
      std::vector<uint8_t> full_payload;

      full_payload.reserve(frame->total_chunks * MAX_CHUNK_SIZE);

      for (uint16_t i = 0; i < frame->total_chunks; ++i) {
        full_payload.insert(full_payload.end(), frame->chunks[i].begin(),
                            frame->chunks[i].end());

        if (full_payload.size() > MAX_FRAME_SIZE) {
          send_debug("Frame size overflow");

          // Overflow, mark frame as invalid
          frame->invalid = true;

          return std::nullopt;
        }
      }

      auto packet = Packet(frame->type, std::move(full_payload));

      // Remove frame from table
      frame_table.erase(std::remove_if(frame_table.begin(), frame_table.end(),
                                       [frame_id](const Frame &f) {
                                         return f.frame_id == frame_id;
                                       }),
                        frame_table.end());

      return packet;
    }

    // Not complete yet
    return std::nullopt;
  }

  void process_rx_buffer() {
    size_t idx;

    while (true) {
      idx = std::find(rx_buffer.begin(), rx_buffer.end(), 0x00) - rx_buffer.begin();

      if (idx == rx_buffer.size()) {
        // No complete chunk yet
        break;
      }

      if (idx > 0) {
        std::array<uint8_t, 4096> decoded{};
        size_t decoded_length = 0;

        if (cobs::decode(rx_buffer.data(), idx, decoded.data(), decoded_length)) {
          if (auto packet = recv(decoded.data(), decoded_length)) {
            on_recv(std::move(packet.value()));
          }
        } else {
          send_debug("COBS decode error");
        }
      }

      // Remove processed chunk and delimiter
      rx_buffer.erase(rx_buffer.begin(), rx_buffer.begin() + idx + 1);
    }
  }

  Frame *get_or_create_frame(uint16_t type, uint32_t frame_id,
                             uint16_t total_chunks) {
    for (auto &e : frame_table) {
      if (e.frame_id != frame_id)
        continue;

      if (e.type != type) {
        // Mark as invalid if type doesn't match
        e.invalid = true;

        return nullptr;
      }

      return &e;
    }

    frame_table.emplace_back(type, frame_id, total_chunks);

    return &frame_table.back();
  }

  void cleanup_stale_frames() {
    const auto now = millis();

    for (auto it = frame_table.begin(); it != frame_table.end();) {
      if (it->invalid || now - it->last_update_ms > RX_FRAME_TIMEOUT_MS) {
        send_debug("Cleaning up stale/invalid frame id: " +
                   std::to_string(it->frame_id));

        it = frame_table.erase(it);
      } else {
        ++it;
      }
    }
  }

  void send_chunk(const uint16_t type, const uint32_t frame_id,
                  const uint16_t total_chunks, const uint16_t chunk_index,
                  const uint8_t *data, const uint16_t len) {
    static constexpr uint8_t delimiter = 0x00;

    assert(len <= MAX_CHUNK_SIZE);

    std::vector<uint8_t> buffer;
    const bytes::Encoder encoder(buffer);

    encoder.push_number(type);
    encoder.push_number(frame_id);
    encoder.push_number(total_chunks);
    encoder.push_number(chunk_index);
    encoder.push_number(len);

    encoder.push_bytes(data, len);

    const auto crc = crc16_ccitt::compute(data, len);

    encoder.push_number(crc);

    std::array<uint8_t, 4096> cobs_buffer{};

    const auto cobs_len =
        cobs::encode(buffer.data(), buffer.size(), cobs_buffer.data());

    cobs_buffer[cobs_len] = 0;

    // write encoded then 0x00 as delimiter
    send_raw_data(cobs_buffer.data(), cobs_len + 1);
  }

  void send_frame(const uint16_t type, const uint32_t frame_id,
                  const uint8_t *payload, const size_t payload_len) {
    assert(payload_len <= MAX_FRAME_SIZE);

    uint16_t total_chunks = (payload_len + MAX_CHUNK_SIZE - 1) / MAX_CHUNK_SIZE;

    if (total_chunks == 0)
      total_chunks = 1;

    for (uint16_t i = 0; i < total_chunks; ++i) {
      const size_t offset = static_cast<size_t>(i) * MAX_CHUNK_SIZE;
      const uint16_t min = std::min(payload_len - offset, MAX_CHUNK_SIZE);

      send_chunk(type, frame_id, total_chunks, i, payload + offset, min);
    }
  }
};

class SerialUSBSocket : public Socket {
public:
  bool is_available() override {
    // DTR status indicates if the host is connected
    return tu_bit_test(tud_cdc_n_get_line_state(0), 0);
  }

private:
  void recv_raw_data(std::vector<uint8_t> &data) override {
    uint32_t avail;

    while ((avail = tud_cdc_available()) > 0) {
      data.resize(data.size() + avail);

      const size_t offset = data.size() - avail;

      tud_task();
      tud_cdc_read(data.data() + offset, avail);
    }
  }

  void send_raw_data(const uint8_t *data, const size_t len) override {
    uint32_t written = 0;

    while (written < len) {
      uint32_t avail = tud_cdc_write_available();

      if (avail > 0) {
        const uint32_t to_write = std::min(avail, static_cast<uint32_t>(len - written));

        tud_task();
        written += tud_cdc_write(data + written, to_write);

        tud_cdc_write_flush();
      }
    }
  }
};

} // namespace packets

enum class PacketType : uint16_t {
  HostHello = 0x0001,
  DeviceHello = 0x0002,
  HostAck = 0x0003,
  Data = 0x0004,
  Error = 0x0005,
};

enum class ReservedErrorCode : uint16_t {
  UnknownPacketType = 0x0001,
  MalformedPacket = 0x0002,
  UnsupportedProtocolVersion = 0x0003,
  MissingCapabilities = 0x0004,
  HandshakeNotCompleted = 0x0005,
  InternalError = 0x00FF,
};

class ISerializable {
public:
  virtual ~ISerializable() = default;

  virtual void serialize(const bytes::Encoder &encoder) const = 0;
};

class IDeserializable {
public:
  virtual ~IDeserializable() = default;

  virtual bool deserialize(bytes::Decoder &decoder) = 0;
};

class ICapability : public ISerializable, public IDeserializable {
public:
  [[nodiscard]] virtual uint16_t id() const = 0;

  [[nodiscard]] virtual uint16_t min_size() const = 0;
};

class SerialCommunicator final : public packets::SerialUSBSocket {
public:
  static constexpr uint16_t VERSION = 410;

  using DataCallback = std::function<void(std::vector<uint8_t> data)>;

  [[nodiscard]] bool is_connected() const {
    return current_handshake_stage == HandshakeStage::Completed;
  }

  void on_unavailable() override {
    if (disconnect_callback) {
      disconnect_callback();
    }

    if constexpr (SOFTWARE_RESET_ON_DISCONNECT) {
      watchdog_reboot(0, SRAM_END, 10);
    }
  }

  void on_recv(packets::Packet packet) override {
    const auto type = static_cast<PacketType>(packet.type);

    if (type == PacketType::Error) {
      const auto &payload = packet.payload;

      if (payload.size() < 2) {
        // Malformed error packet
        return;
      }

      bytes::Decoder decoder{payload.data(), payload.size()};

      const auto error_code = decoder.pop_number<uint16_t>();
      const auto it = find_data_callback(error_code, error_callbacks);

      if (it == error_callbacks.end()) {
        // No callback registered for this error code
        return;
      }

      std::vector<uint8_t> data(decoder.remaining());

      decoder.pop_bytes(data.data(), data.size());

      it->second(std::move(data));

      return;
    }

    if (!is_connected()) {
      if (!process_handshake(type, packet)) {
        send_error(
          static_cast<uint16_t>(ReservedErrorCode::HandshakeNotCompleted)
        );
      }

      // Set handshake status led
      if (current_handshake_stage == HandshakeStage::Completed) {
        digitalWrite(LED_BUILTIN, LOW);
      } else if (current_handshake_stage != HandshakeStage::None) {
        digitalWrite(LED_BUILTIN, HIGH);
      }

      return;
    }

    if (type == PacketType::Data) {
      const auto &payload = packet.payload;

      if (payload.size() < 2) {
        // Malformed error packet
        return;
      }

      bytes::Decoder decoder{payload.data(), payload.size()};

      const auto data_code = decoder.pop_number<uint16_t>();
      const auto it = find_data_callback(data_code, data_callbacks);

      if (it == data_callbacks.end()) {
        // No callback registered for this error code
        return;
      }

      std::vector<uint8_t> data(decoder.remaining());

      decoder.pop_bytes(data.data(), data.size());

      it->second(std::move(data));

      return;
    } else {
      send_debug("Received unknown packet: " + std::to_string(static_cast<uint16_t>(type)));

      send_error(static_cast<uint16_t>(ReservedErrorCode::UnknownPacketType));
    }
  }

  // Handshake

  void add_capability(ICapability &host_cap, const ICapability &device_cap) {
    host_capabilities.push_back(std::ref(host_cap));
    device_capabilities.push_back(std::cref(device_cap));
  }

  void on_disconnect(std::function<void()> &&fn) {
    disconnect_callback = std::move(fn);
  }

  // Receiving

  void subscribe_data(const uint16_t code, DataCallback &&callback) {
    add_data_callback(code, std::move(callback), data_callbacks);
  }

  void unsubscribe_data(const uint16_t code) {
    const auto it = find_data_callback(code, data_callbacks);

    if (it != data_callbacks.end()) {
      data_callbacks.erase(it);
    }
  }

  void subscribe_error(const uint16_t code, DataCallback &&callback) {
    add_data_callback(code, std::move(callback), error_callbacks);
  }

  void unsubscribe_error(const uint16_t code) {
    const auto it = find_data_callback(code, error_callbacks);

    if (it != error_callbacks.end()) {
      error_callbacks.erase(it);
    }
  }

  // Sending

  void send_data(const uint16_t code,
                 const std::vector<uint8_t> &data_payload = {}) {
    if (!is_connected())
      return;

    std::vector<uint8_t> payload;

    const bytes::Encoder encoder{payload};

    encoder.push_number(code);
    encoder.push_bytes(data_payload.data(), data_payload.size());

    send(packets::Packet(static_cast<uint16_t>(PacketType::Data),
                         std::move(payload)));
  }

  void send_data(const uint16_t code, const ISerializable &data) {
    if (!is_connected())
      return;

    std::vector<uint8_t> payload;

    const bytes::Encoder encoder{payload};

    encoder.push_number(code);
    data.serialize(encoder);

    send(packets::Packet(static_cast<uint16_t>(PacketType::Data),
                         std::move(payload)));
  }

  void send_error(const uint16_t code,
                  const std::vector<uint8_t> &error_payload = {}) {
    std::vector<uint8_t> payload;

    const bytes::Encoder encoder{payload};

    encoder.push_number(code);
    encoder.push_bytes(error_payload.data(), error_payload.size());

    send(packets::Packet(static_cast<uint16_t>(PacketType::Error),
                         std::move(payload)));
  }

  void send_error(const uint16_t code, const ISerializable &data) {
    std::vector<uint8_t> payload;

    const bytes::Encoder encoder{payload};

    encoder.push_number(code);
    data.serialize(encoder);

    send(packets::Packet(static_cast<uint16_t>(PacketType::Error),
                         std::move(payload)));
  }

private:
  using data_callbacks_t = std::vector<std::pair<uint16_t, DataCallback>>;

  enum class HandshakeStage {
    None,
    HostHelloReceived,
    DeviceHelloSent,
    Completed,
  };

  HandshakeStage current_handshake_stage = HandshakeStage::None;
  std::vector<std::reference_wrapper<ICapability>> host_capabilities;
  std::vector<std::reference_wrapper<const ICapability>> device_capabilities;

  std::function<void()> disconnect_callback;

  data_callbacks_t data_callbacks;
  data_callbacks_t error_callbacks;

  static data_callbacks_t::iterator
  find_data_callback(const uint16_t type, data_callbacks_t &callbacks) {
    const auto it = std::lower_bound(
        callbacks.begin(), callbacks.end(), type,
        [](const auto &a, const auto &b) { return a.first < b; });

    if (it == callbacks.end() || it->first != type) {
      return callbacks.end();
    }

    return it;
  }

  static void add_data_callback(const uint16_t type, DataCallback &&callback,
                                data_callbacks_t &callbacks) {
    const auto it = find_data_callback(type, callbacks);

    if (it != callbacks.end() && it->first == type) {
      // Replace existing
      it->second = callback;

      return;
    }

    callbacks.emplace_back(type, std::move(callback));

    std::sort(callbacks.begin(), callbacks.end(),
              [](const auto &a, const auto &b) { return a.first < b.first; });
  }

  [[nodiscard]] std::optional<ReservedErrorCode>
  process_host_hello(const packets::Packet &packet) const {
    auto &payload = packet.payload;

    if (payload.size() < 3) {
      return ReservedErrorCode::MalformedPacket;
    }

    bytes::Decoder decoder(payload.data(), payload.size());

    const auto version = decoder.pop_number<uint16_t>();

    if (version != VERSION) {
      return ReservedErrorCode::UnsupportedProtocolVersion;
    }

    const uint8_t capabilities_count = decoder.pop_byte();

    if (payload.size() < capabilities_count * (2 + 2)) {
      return ReservedErrorCode::MalformedPacket;
    }

    for (size_t i = 0; i < capabilities_count; ++i) {
      const auto cap_id = decoder.pop_number<uint16_t>();
      const auto cap_size = decoder.pop_number<uint16_t>();

      if (payload.size() < cap_size) {
        return ReservedErrorCode::MalformedPacket;
      }

      std::vector<uint8_t> cap_data(cap_size);

      decoder.pop_bytes(cap_data.data(), cap_size);

      const auto cap_it = std::find_if(
          host_capabilities.cbegin(), host_capabilities.cend(),
          [cap_id](const std::reference_wrapper<ICapability> &cap) {
            return cap.get().id() == cap_id;
          });

      if (cap_it == host_capabilities.cend()) {
        return ReservedErrorCode::MissingCapabilities;
      }

      auto &cap = cap_it->get();

      if (cap.min_size() > cap_size) {
        return ReservedErrorCode::MalformedPacket;
      }

      bytes::Decoder cap_decoder(cap_data.data(), cap_size);

      if (!cap.deserialize(cap_decoder)) {
        return ReservedErrorCode::MalformedPacket;
      }
    }

    return std::nullopt;
  }

  void send_device_hello() {
    std::vector<uint8_t> payload;

    const bytes::Encoder encoder(payload);

    const auto device_id = device_id::get();

    encoder.push_number(device_id);
    encoder.push_number(static_cast<uint8_t>(device_capabilities.size()));

    for (const auto device_capability : device_capabilities) {
      const auto &cap = device_capability.get();

      std::vector<uint8_t> cap_payload;

      const bytes::Encoder cap_encoder(cap_payload);

      cap.serialize(cap_encoder);

      encoder.push_number(cap.id());
      encoder.push_number(static_cast<uint16_t>(cap_payload.size()));
      encoder.push_bytes(cap_payload.data(), cap_payload.size());
    }

    send(packets::Packet(static_cast<uint16_t>(PacketType::DeviceHello),
                         std::move(payload)));
  }

  bool process_handshake(PacketType type, packets::Packet &packet) {
    if (current_handshake_stage == HandshakeStage::None) {
      if (type != PacketType::HostHello)
        return true; // Ignore non-handshake packets while not connected

      if (const auto error = process_host_hello(packet)) {
        send_error(static_cast<uint16_t>(error.value()));

        return false;
      }

      current_handshake_stage = HandshakeStage::HostHelloReceived;

      send_device_hello();

      current_handshake_stage = HandshakeStage::DeviceHelloSent;

      return true;
    }

    if (current_handshake_stage == HandshakeStage::DeviceHelloSent) {
      if (type != PacketType::HostAck) {
        send_error(
            static_cast<uint16_t>(ReservedErrorCode::HandshakeNotCompleted));

        return false;
      }

      current_handshake_stage = HandshakeStage::Completed;

      return true;
    }

    return true;
  }
};

#pragma endregion /* Serial Communicator Implementation */

SerialCommunicator comm;

struct DeviceData final : ISerializable {
  bool button_pressing = false;
  int32_t light_strength_average = 0;
  float core_temp_average = 0;

  DeviceData() = default;

  DeviceData(const bool button_pressing, const int32_t light_strength_average,
             const float core_temp_average)
      : button_pressing(button_pressing),
        light_strength_average(light_strength_average),
        core_temp_average(core_temp_average) {}

  void serialize(const bytes::Encoder &encoder) const override {
    encoder.push_bool(button_pressing);
    encoder.push_number(light_strength_average);
    encoder.push_number(core_temp_average);
  }
};

struct ToneData final : IDeserializable {
  float frequency;
  float volume = 1;
  std::optional<uint32_t> duration;

  ToneData() = default;

  ToneData(const float frequency, const float volume = 1,
       const std::optional<uint32_t> duration = std::nullopt)
      : frequency(frequency), volume(volume), duration(duration) {}

  bool deserialize(bytes::Decoder &decoder) override {
    if (decoder.remaining() < (4 + 4 + 4))
      return false;

    frequency = decoder.pop_number<float>();
    volume = decoder.pop_number<float>();

    const auto raw_duration = decoder.pop_number<uint32_t>();

    if (raw_duration > 0)
      duration = raw_duration;
    else
      duration.reset();

    return true;
  }
};

struct RGBColorData final : IDeserializable {
  uint8_t r = 0;
  uint8_t g = 0;
  uint8_t b = 0;

  RGBColorData() = default;

  bool deserialize(bytes::Decoder &decoder) override {
    if (decoder.remaining() < (1 + 1 + 1))
      return false;

    r = decoder.pop_number<uint8_t>();
    g = decoder.pop_number<uint8_t>();
    b = decoder.pop_number<uint8_t>();

    return true;
  }
};

enum class WaveformType : uint16_t {
  Square = 0x0001,
  Square25 = 0x0002,
  Square12 = 0x0003,
  Triangle = 0x0004,
  Saw = 0x0005,
  Sine = 0x0006,
  Noise = 0x0007,
};

volatile bool button_pressing = false;
volatile int32_t light_strength_average = 0;
volatile float core_temp_average = 0;

bool send_data_forever = false;

void wait_for_serial() {
  bool led_state = false;

  while (!Serial) {
    delay(150);

    led_state = !led_state;

    digitalWrite(LED_BUILTIN, led_state ? HIGH : LOW);
  }

  digitalWrite(LED_BUILTIN, LOW);
}

void send_data() {
  const DeviceData device_data{button_pressing, light_strength_average,
                               core_temp_average};

  comm.send_data(static_cast<uint16_t>(DataTypes::ResponseDataSend),
                 device_data);
}

bool get_n_bit(const uint8_t flags, const size_t n) {
  return (flags >> n) & 0b1;
}

static ToneData tone_data;
static RGBColorData color_data;

void process_data(const uint8_t flags, bytes::Decoder &decoder) {
  // 0 bit are currently unused

  if (get_n_bit(flags, 1)) {
    // Change waveform

    if (decoder.remaining() < 2) {
      comm.send_error(
          static_cast<uint16_t>(ReservedErrorCode::MalformedPacket));

      return;
    }

    rp2040.fifo.push_nb(FIFO_WAVEFORM);
    rp2040.fifo.push_nb(static_cast<uint32_t>(decoder.pop_number<uint16_t>()));
  }

  if (get_n_bit(flags, 2)) {
    // No tone
    rp2040.fifo.push_nb(FIFO_NO_TONE);

    return;
  }

  if (get_n_bit(flags, 3)) {
    // Tone

    if (!tone_data.deserialize(decoder)) {
      comm.send_error(
          static_cast<uint16_t>(ReservedErrorCode::MalformedPacket));

      return;
    }

    rp2040.fifo.push_nb(FIFO_TONE);
    rp2040.fifo.push_nb(reinterpret_cast<uint32_t>(&tone_data));
  }

  if (get_n_bit(flags, 4)) {
    // LED Builtin
    if (decoder.remaining() < 1) {
      comm.send_error(
          static_cast<uint16_t>(ReservedErrorCode::MalformedPacket));

      return;
    }

    const auto led_on = decoder.pop_bool();

    rp2040.fifo.push_nb(FIFO_LED_BUILTIN);
    rp2040.fifo.push_nb(led_on);
  }

  if (get_n_bit(flags, 5)) {
    // RGB LED

    if (!color_data.deserialize(decoder)) {
      comm.send_error(
          static_cast<uint16_t>(ReservedErrorCode::MalformedPacket));

      return;
    }

    rp2040.fifo.push_nb(FIFO_RGB_LED);
    rp2040.fifo.push_nb(reinterpret_cast<uint32_t>(&color_data));
  }
}

class FraiselaitDeviceCapability : public ICapability {
public:
  FraiselaitDeviceCapability() = default;

  [[nodiscard]] uint16_t id() const override { return 0x0040; }

  [[nodiscard]] uint16_t min_size() const override { return 0; }

  void serialize(const bytes::Encoder &encoder) const override {
  }

  bool deserialize(bytes::Decoder &encoder) override {
    return true;
  }
};

FraiselaitDeviceCapability fraiselaitDeviceCap;

void reset_state() {
  tone_data.frequency = 0;
  tone_data.volume = 1;
  tone_data.duration.reset();

  color_data.r = 0;
  color_data.g = 0;
  color_data.b = 0;

  send_data_forever = false;

  rp2040.fifo.push_nb(FIFO_NO_TONE);
  rp2040.fifo.push_nb(FIFO_RGB_LED);
  rp2040.fifo.push_nb(reinterpret_cast<uint32_t>(&color_data));
}

void setup() {
  // 460800 bps, 8 data bits, no parity, 1 stop bit
  Serial.begin(460800, SERIAL_8N1);

  wait_for_serial();

  comm.add_capability(fraiselaitDeviceCap, fraiselaitDeviceCap);

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataGetImmediate),
    [](auto) { send_data(); }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataGetLoopOff),
    [](auto) { send_data_forever = false; }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataGetLoopOn),
    [](auto) { send_data_forever = true; }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataSet),
    [](std::vector<uint8_t> payload) {
      if (payload.empty()) {
        comm.send_error(static_cast<uint16_t>(
            ReservedErrorCode::MalformedPacket));

        return;
      }

      bytes::Decoder decoder{payload.data(), payload.size()};
      const auto flags = decoder.pop_number<uint8_t>();

      process_data(flags, decoder);
    }
  );

  comm.on_disconnect(reset_state);
}

void loop() {
  comm.update();

  if (!comm.is_connected())
    return;

  if (send_data_forever) {
    send_data();
  }
}

void smooth_analog_values() {
  static size_t light_strength_read_index = 0;
  static int32_t light_strength_total = 0;
  static std::array<int32_t, ANALOG_READINGS> light_strength_readings{};
  static std::array<float, ANALOG_READINGS> core_temp_readings{};

  light_strength_total -= light_strength_readings[light_strength_read_index];
  light_strength_readings[light_strength_read_index] =
      analogRead(PIN_LIGHT_SENSOR);
  light_strength_total += light_strength_readings[light_strength_read_index];
  light_strength_read_index++;

  if (light_strength_read_index >= light_strength_readings.size()) {
    light_strength_read_index = 0;
  }

  light_strength_average =
      light_strength_total / light_strength_readings.size();

  static size_t core_temp_read_index = 0;
  static float core_temp_total = 0;

  core_temp_total -= core_temp_readings[core_temp_read_index];
  core_temp_readings[core_temp_read_index] = analogReadTemp();
  core_temp_total += core_temp_readings[core_temp_read_index];
  core_temp_read_index++;

  if (core_temp_read_index >= core_temp_readings.size()) {
    core_temp_read_index = 0;
  }

  core_temp_average = core_temp_total / core_temp_readings.size();
}

static dynamic_tone::Speaker sp{PIN_SPEAKER, true};

void command_no_tone() { sp.stop(); }

void command_tone(const ToneData &data) {
  sp.set_frequency(data.frequency);
  sp.set_volume(data.volume);

  if (data.duration) {
    sp.play(data.duration.value());
  } else {
    sp.play();
  }
}

void command_change_color(const RGBColorData &data) {
  analogWrite(PIN_LED_RED, data.r);
  analogWrite(PIN_LED_GREEN, data.g);
  analogWrite(PIN_LED_BLUE, data.b);
}

void command_change_led_builtin(const bool data) {
  digitalWrite(LED_BUILTIN, data);
}

void command_change_waveform(WaveformType type) {
  switch (type) {
    case WaveformType::Square:
      sp.set_waveform(dynamic_tone::SQUARE_WAVEFORM);

      break;

    case WaveformType::Square25:
      sp.set_waveform(dynamic_tone::SQUARE_25_WAVEFORM);

      break;

    case WaveformType::Square12:
      sp.set_waveform(dynamic_tone::SQUARE_12_WAVEFORM);

      break;

    case WaveformType::Triangle:
      sp.set_waveform(dynamic_tone::TRIANGLE_WAVEFORM);

      break;

    case WaveformType::Saw:
      sp.set_waveform(dynamic_tone::SAW_WAVEFORM);

      break;

    case WaveformType::Sine:
      sp.set_waveform(dynamic_tone::SINE_WAVEFORM);

      break;

    case WaveformType::Noise:
      sp.set_waveform(dynamic_tone::NOISE_WAVEFORM);

      break;

    default:
      break;
  }
}

void setup1() {
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(PIN_SPEAKER, OUTPUT);
  pinMode(PIN_TACT_SWITCH, INPUT_PULLUP);
  pinMode(PIN_LED_GREEN, OUTPUT);
  pinMode(PIN_LED_BLUE, OUTPUT);
  pinMode(PIN_LED_RED, OUTPUT);
  pinMode(PIN_LIGHT_SENSOR, INPUT);
}

void loop1() {
  if (uint32_t cmd; rp2040.fifo.pop_nb(&cmd)) {
    if (cmd == FIFO_NO_TONE) {
      command_no_tone();
    } else if (cmd == FIFO_TONE) {
      command_tone(*reinterpret_cast<ToneData *>(rp2040.fifo.pop()));
    } else if (cmd == FIFO_LED_BUILTIN) {
      command_change_led_builtin(rp2040.fifo.pop());
    } else if (cmd == FIFO_RGB_LED) {
      command_change_color(*reinterpret_cast<RGBColorData *>(rp2040.fifo.pop()));
    } else if (cmd == FIFO_WAVEFORM) {
      command_change_waveform(static_cast<WaveformType>(rp2040.fifo.pop()));
    }
  }

  smooth_analog_values();
  button_pressing = digitalRead(PIN_TACT_SWITCH) == LOW;
}
