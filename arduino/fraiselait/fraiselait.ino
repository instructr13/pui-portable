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
constexpr bool     MORE_DEBUG_LOG = true;

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

#pragma region Packet Communicator Implementation

/*
 * Packet Model:
 * - All numbers are Big Endian
 * - Packet structure (encapsulated with COBS):
 *  1. CRC8 (1 byte)
 *  2. Packet Type (2 byte)
 *  3. Payload (variable length)
 *
 * Packet Types:
 * 1. HostHello (0x01): Sent by the host to initiate a handshake.
 *    Payload: Protocol Version (2 byte) + Required Capability Array
 * 2. DeviceHello (0x02): Sent by the device in response to HostHello.
 *    Payload: Device ID (4 byte) + Supported Capability Array
 * 3. HostAck (0x03): Sent by the host to acknowledge DeviceHello.
 *    Payload: None
 * 4. Data (0x04): General data packet for communication.
 *    Payload: Data Type (2 byte) + Data (variable length)
 * 5. Error (0x05): Sent by either side to indicate an error.
 *    Payload: Error Code (2 byte) + Error Message (variable length)
 * 6. DebugEcho (0x06): Sent by the device to echo debug messages.
 *    Payload: Debug Message (variable length)
 *
 * Components:
 * - Capability: A single capability represented as a byte.
 *   Type (2 byte) + Size (2 byte) + Data (variable length)
 * - Capability Array: A list of capabilities supported or required by the
 * device/host.
 *   Size (1 byte) + [Capability]...
 *
 * To Disconnect: Simply close the serial connection or set DTR to 0 on the
 * host.
 */

namespace cobs {

enum class DecodeStatus {
  InProgress,
  Complete,
  Error,
};

std::vector<std::byte> encode(const std::vector<std::byte> &in) {
  assert(!in.empty());

  std::vector<std::byte> ret;
  ret.reserve(in.size() + in.size() / 254 + 2);

  ret.push_back(std::byte{0}); // Placeholder for code

  auto dst_it = ret.begin() + 1;
  auto code_it = ret.begin();

  uint8_t code = 0x01;

  for (const auto b : in) {
    if (b == std::byte{0}) {
      *code_it = std::byte{code};
      code_it = dst_it;

      ret.emplace_back(std::byte{0x00});

      ++dst_it;

      code = 0x01;

      continue;
    }

    ret.push_back(b);

    ++dst_it;
    ++code;

    if (code == 0xFF) {
      *code_it = std::byte{code};
      code_it = dst_it;

      ret.emplace_back(std::byte{0x00});

      ++dst_it;

      code = 0x01;
    }
  }

  *code_it = std::byte{code};

  return ret;
}

DecodeStatus decode(std::vector<std::byte> &in, std::vector<std::byte> &out) {
  for (auto it = in.begin(); it != in.end();) {
    const auto code = std::to_integer<uint8_t>(*it++);

    if (code == 0) {
      in.erase(in.begin(), it);

      // Invalid COBS
      if (it == in.end()) {
        return DecodeStatus::Error;
      }

      return DecodeStatus::Complete;
    }

    for (size_t i = 1; i < code; ++i) {
      if (it == in.end()) {
        in.erase(in.begin(), it);

        // Not enough data
        return DecodeStatus::InProgress;
      }

      out.push_back(*it++);
    }

    if (code != 0xFF) {
      if (it != in.end() && std::to_integer<uint8_t>(*it) != 0) {
        out.push_back(std::byte{0});
      }

    }
  }

  in.clear();

  return DecodeStatus::InProgress;
}

} // namespace cobs

namespace crc8 {

uint8_t compute(const std::vector<std::byte>::const_iterator begin,
                const std::vector<std::byte>::const_iterator end) {
  static constexpr uint8_t POLYNOMIAL = 0x07;

  uint8_t crc = 0x00;

  for (auto it = begin; it < end; ++it) {
    const auto b = *it;

    crc ^= std::to_integer<uint8_t>(b);

    for (size_t i = 0; i < 8; ++i) {
      if (crc & 0x80) {
        crc = (crc << 1) ^ POLYNOMIAL; // Polynomial x^8 + x^2 + x + 1
      } else {
        crc <<= 1;
      }
    }
  }

  return crc;
}

bool verify(const std::vector<std::byte>::const_iterator begin,
            const std::vector<std::byte>::const_iterator end,
            const uint8_t expected_crc) {
  return compute(begin, end) == expected_crc;
}

} // namespace crc8

class PacketEncoder {
public:
  explicit PacketEncoder(std::vector<std::byte> &buf) : buffer(buf) {}

  void push_byte(const std::byte b) const { buffer.push_back(b); }

  void push_bytes(const std::byte *data, const size_t len) const {
    buffer.insert(buffer.end(), data, data + len);
  }

  template <typename T> void push_number(const T v) const {
    static_assert(std::is_integral_v<T> || std::is_floating_point_v<T>);

    if constexpr (std::is_integral_v<T>) {
      for (int i = sizeof(T) - 1; i >= 0; --i) {
        push_byte(static_cast<std::byte>((v >> (i * 8)) & 0xFF));
      }
    } else if constexpr (std::is_floating_point_v<T>) {
      if constexpr (sizeof(T) == 4) {
        const auto as_int = *reinterpret_cast<const uint32_t *>(&v);

        push_number(as_int);
      } else if constexpr (sizeof(T) == 8) {
        const auto as_int = *reinterpret_cast<const uint64_t *>(&v);

        push_number(as_int);
      }
    }
  }

  void push_bool(const bool v) const {
    push_byte(v ? std::byte{0xFF} : std::byte{0});
  }

  void push_string(const std::string &str) const {
    push_bytes(reinterpret_cast<const std::byte *>(str.data()), str.size());
  }

private:
  std::vector<std::byte> &buffer;
};

class PacketDecoder {
public:
  explicit PacketDecoder(std::vector<std::byte> &buffer) : buffer(buffer) {}

  [[nodiscard]] size_t remaining() const { return buffer.size(); }

  std::byte pop_byte() const {
    assert(!buffer.empty());

    const auto b = buffer.front();

    buffer.erase(buffer.begin());

    return b;
  }

  std::vector<std::byte> pop_bytes(const size_t len) const {
    assert(buffer.size() >= len);

    std::vector<std::byte> bytes{len};

    std::copy_n(buffer.begin(), len, bytes.begin());

    buffer.erase(buffer.begin(), buffer.begin() + len);

    return bytes;
  }

  uint8_t pop_uint8() const { return std::to_integer<uint8_t>(pop_byte()); }

  uint16_t pop_uint16() const {
    assert(buffer.size() >= 2);

    return (static_cast<uint16_t>(std::to_integer<uint8_t>(pop_byte())) << 8) |
           static_cast<uint16_t>(std::to_integer<uint8_t>(pop_byte()));
  }

  uint32_t pop_uint32() const {
    assert(buffer.size() >= 4);

    return (static_cast<uint32_t>(std::to_integer<uint8_t>(pop_byte())) << 24) |
           (static_cast<uint32_t>(std::to_integer<uint8_t>(pop_byte())) << 16) |
           (static_cast<uint32_t>(std::to_integer<uint8_t>(pop_byte())) << 8) |
           static_cast<uint32_t>(std::to_integer<uint8_t>(pop_byte()));
  }

  float pop_float() const {
    assert(buffer.size() >= 4);
    static_assert(sizeof(float) == sizeof(uint32_t));

    const auto raw_bytes = pop_uint32();
    float ret;

    std::memcpy(&ret, &raw_bytes, sizeof(float));

    return ret;
  }

  std::string pop_string(const size_t len) const {
    assert(buffer.size() >= len);

    const auto bytes = pop_bytes(len);

    return {reinterpret_cast<const char *>(bytes.data()), bytes.size()};
  }

private:
  std::vector<std::byte> &buffer;
};

enum class PacketType : uint16_t {
  HostHello = 0x0001,
  DeviceHello = 0x0002,
  HostAck = 0x0003,
  Data = 0x0004,
  Error = 0x0005,
  DebugEcho = 0x0006,
};

enum class ReservedErrorCode : uint16_t {
  UnknownPacketType = 0x0001,
  MalformedPacket = 0x0002,
  UnsupportedProtocolVersion = 0x0003,
  MissingCapabilities = 0x0004,
  HandshakeNotCompleted = 0x0005,
  InternalError = 0x00FF,
};

class Packet {
public:
  Packet(const uint16_t type, std::vector<std::byte> &&payload)
      : type_(type), payload_(std::move(payload)) {}

  static std::optional<Packet> parse(std::vector<std::byte> &data) {
    if (data.size() < 3) // Minimum size: 1 byte crc + 2 byte type
      return std::nullopt;

    const PacketDecoder decoder{data};

    const auto crc = decoder.pop_byte();

    if (!crc8::verify(data.cbegin() + 1, data.cend(), std::to_integer<uint8_t>(crc)))
      return std::nullopt;

    const auto type = decoder.pop_uint16();
    const auto size = data.size();

    std::vector<std::byte> payload{size};

    std::copy(data.cbegin(), data.cend(), payload.begin());

    data.clear();

    return Packet{type, std::move(payload)};
  }

  std::vector<std::byte> encode() const {
    std::vector<std::byte> raw_payload;

    {
      const PacketEncoder encoder{raw_payload};

      encoder.push_number(type_);
      encoder.push_bytes(payload_.data(), payload_.size());
    }

    const auto crc = crc8::compute(raw_payload.cbegin(), raw_payload.cend());

    std::vector<std::byte> packet_data;
    packet_data.reserve(1 + raw_payload.size() + 1);

    {
      const PacketEncoder encoder{packet_data};

      encoder.push_byte(std::byte{crc});
      encoder.push_bytes(raw_payload.data(), raw_payload.size());
    }

    return packet_data;
  }

  [[nodiscard]] uint16_t type() const { return type_; }

  std::vector<std::byte> &payload() { return payload_; }

  [[nodiscard]] const std::vector<std::byte> &payload() const {
    return payload_;
  }

private:
  uint16_t type_;
  std::vector<std::byte> payload_;
};

class ISerializable {
public:
  virtual ~ISerializable() = default;

  virtual void serialize(const PacketEncoder &encoder) const = 0;
};

class IDeserializable {
public:
  virtual ~IDeserializable() = default;

  virtual bool deserialize(const PacketDecoder &decoder) = 0;
};

namespace capability {

class ICapability : public ISerializable, public IDeserializable {
public:
  virtual ~ICapability() = default;

  [[nodiscard]] virtual uint16_t id() const = 0;

  [[nodiscard]] virtual uint16_t min_size() const = 0;
};

} // namespace capability

class PacketCommunicator {
public:
  static constexpr uint16_t VERSION = 400;

  using DataCallback = std::function<void(std::vector<std::byte> &data)>;

  PacketCommunicator() {
    rx_buffer.reserve(256);
    rx_cobs.reserve(256);
  };

  void loop() {
    // If no USB connection or DTR, clear buffers and mark disconnected
    if (!is_dtr_ready() && !wait_connection) {
      receive_to_rx_buf(); // Consume Serial buffer

      rx_buffer.clear();
      rx_cobs.clear();
      rx_packet.reset();

      current_handshake = HandshakeStage::None;

      wait_connection = true;

      disconnect_callback();

      if constexpr (SOFTWARE_RESET_ON_DISCONNECT) {
        watchdog_reboot(0, SRAM_END, 10);
      }

      return;
    };

    wait_connection = false;

    if (Serial.available() == 0) return;

    receive_to_rx_buf();

    const auto status = cobs::decode(rx_buffer, rx_cobs);

    rx_buffer.clear();

    if (status == cobs::DecodeStatus::Error) {
      if constexpr (MORE_DEBUG_LOG) {
        send_debug("COBS decode error");
      }

      // COBS error, clear packet buffer
      if (!rx_cobs.empty())
        rx_cobs.clear();

      return;
    }

    if (status == cobs::DecodeStatus::InProgress) {
      // Still waiting for more data
      return;
    }

    rx_packet = Packet::parse(rx_cobs);

    rx_cobs.clear();

    if (!rx_packet) {
      if constexpr (MORE_DEBUG_LOG) {
        send_debug("Packet parse error");
      }

      return;
    }

    const auto type = static_cast<PacketType>(rx_packet->type());

    if (type == PacketType::DebugEcho) {
      // Drop all debug echo from host
      return;
    }

    if (type == PacketType::Error) {
      const auto &payload = rx_packet->payload();

      if (payload.size() < 2) {
        // Malformed error packet
        return;
      }

      const PacketDecoder decoder{rx_packet->payload()};

      const auto error_code = decoder.pop_uint16();
      const auto it = find_data_callback(error_code, error_callbacks);

      if (it == error_callbacks.end()) {
        // No callback registered for this error code
        return;
      }

      std::vector<std::byte> data{payload.size()};

      std::copy(payload.begin(), payload.end(), data.begin());

      it->second(data);

      return;
    }

    if (!is_connected()) {
      if (!process_handshake()) {
        send_error(
          static_cast<uint16_t>(ReservedErrorCode::HandshakeNotCompleted)
        );
      }

      // Set handshake status led
      if (current_handshake == HandshakeStage::Completed) {
        digitalWrite(LED_BUILTIN, LOW);
      } else if (current_handshake != HandshakeStage::None) {
        digitalWrite(LED_BUILTIN, HIGH);
      }

      return;
    }

    if (type == PacketType::Data) {
      const auto &payload = rx_packet->payload();

      if (payload.size() < 2) {
        send_error(static_cast<uint16_t>(ReservedErrorCode::MalformedPacket));

        return;
      }

      const PacketDecoder decoder{rx_packet->payload()};

      const auto data_type = decoder.pop_uint16();

      const auto it = find_data_callback(data_type, data_callbacks);

      if (it == data_callbacks.end()) {
        // No callback registered for this data type
        return;
      }

      std::vector<std::byte> data{payload.size()};

      std::copy(payload.begin(), payload.end(), data.begin());

      it->second(data);
    } else {
      send_error(static_cast<uint16_t>(ReservedErrorCode::UnknownPacketType));
    }
  }

  // Handshake

  void add_capability(capability::ICapability &host_cap, const capability::ICapability &device_cap) {
    host_capabilities.push_back(std::ref(host_cap));
    device_capabilities.push_back(std::cref(device_cap));
  }

  void on_disconnect(std::function<void()> &&fn) {
    disconnect_callback = std::move(fn);
  }

  // Receiving

  void subscribe_data(const uint16_t data_type, const DataCallback &callback) {
    const auto it = find_data_callback(data_type, data_callbacks);

    if (it == data_callbacks.end()) {
      data_callbacks.emplace_back(data_type, callback);

      std::sort(data_callbacks.begin(), data_callbacks.end(),
                [](const auto &a, const auto &b) { return a.first < b.first; });

      return;
    }

    it->second = callback;
  }

  void unsubscribe_data(const uint16_t data_type) {
    const auto it = find_data_callback(data_type, data_callbacks);

    if (it == data_callbacks.end()) {
      return;
    }

    data_callbacks.erase(it);
  }

  void subscribe_error(const uint16_t error_code,
                       const DataCallback &callback) {
    const auto it = find_data_callback(error_code, error_callbacks);

    if (it == error_callbacks.end()) {
      error_callbacks.emplace_back(error_code, callback);

      std::sort(error_callbacks.begin(), error_callbacks.end(),
                [](const auto &a, const auto &b) { return a.first < b.first; });

      return;
    }

    it->second = callback;
  }

  void unsubscribe_error(const uint16_t error_code) {
    const auto it = find_data_callback(error_code, error_callbacks);

    if (it == error_callbacks.end()) {
      return;
    }

    error_callbacks.erase(it);
  }

  // Sending

  void send_data(const uint16_t data_type,
                 const std::vector<std::byte> &data_payload = {}) const {
    if (!is_connected())
      return;

    std::vector<std::byte> payload;

    {
      const PacketEncoder encoder{payload};

      encoder.push_number(data_type);
      encoder.push_bytes(data_payload.data(), data_payload.size());
    }

    const Packet packet{static_cast<uint16_t>(PacketType::Data),
                        std::move(payload)};

    send_packet(packet);
  }

  void send_data(const uint16_t data_type, const ISerializable &data) const {
    if (!is_connected())
      return;

    std::vector<std::byte> payload;

    {
      const PacketEncoder encoder{payload};

      encoder.push_number(data_type);
      data.serialize(encoder);
    }

    const Packet packet{static_cast<uint16_t>(PacketType::Data),
                        std::move(payload)};

    send_packet(packet);
  }

  void send_error(const uint16_t code,
                  const std::vector<std::byte> &error_payload = {}) const {
    std::vector<std::byte> payload;

    {
      const PacketEncoder encoder{payload};

      encoder.push_number(code);
      encoder.push_bytes(error_payload.data(), error_payload.size());
    }

    const Packet packet{static_cast<uint16_t>(PacketType::Error),
                        std::move(payload)};

    send_packet(packet);
  }

  void send_error(const uint16_t code, const ISerializable &data) const {
    std::vector<std::byte> payload;

    {
      const PacketEncoder encoder{payload};

      encoder.push_number(code);
      data.serialize(encoder);
    }

    const Packet packet{static_cast<uint16_t>(PacketType::Error),
                        std::move(payload)};

    send_packet(packet);
  }

  void send_debug(const std::string &str) const {
    std::vector<std::byte> payload;

    const PacketEncoder encoder{payload};

    encoder.push_string(str);

    const Packet packet{static_cast<uint16_t>(PacketType::DebugEcho),
                        std::move(payload)};

    send_packet(packet);
  }

  [[nodiscard]] bool is_waiting_connection() const {
    return wait_connection;
  }

  [[nodiscard]] bool is_connected() const {
    return current_handshake == HandshakeStage::Completed;
  }

private:
  using data_callbacks_t = std::vector<std::pair<uint16_t, DataCallback>>;

  enum class HandshakeStage {
    None,
    HostHelloReceived,
    DeviceHelloSent,
    Completed,
  };

  static bool is_dtr_ready() {
    return tu_bit_test(tud_cdc_n_get_line_state(0), 0);
  }

  static void send_packet(const Packet &packet) {
    const auto raw_data = packet.encode();
    const auto cobs_data = cobs::encode(raw_data);

    Serial.write(reinterpret_cast<const uint8_t *>(cobs_data.data()),
                 cobs_data.size());
  }

  static data_callbacks_t::iterator
  find_data_callback(const uint16_t type, data_callbacks_t &callbacks) {
    const auto it = std::lower_bound(
      callbacks.begin(), callbacks.end(), type,
      [](const auto &pair, const auto &value) { return pair.first < value; }
    );

    if (it == callbacks.end() || it->first != type) {
      return callbacks.end();
    }

    return it;
  }

  std::vector<std::byte> rx_buffer;
  std::vector<std::byte> rx_cobs;
  std::optional<Packet> rx_packet;

  bool wait_connection = false;
  HandshakeStage current_handshake = HandshakeStage::None;
  std::vector<std::reference_wrapper<capability::ICapability>> host_capabilities;
  std::vector<std::reference_wrapper<const capability::ICapability>> device_capabilities;

  data_callbacks_t data_callbacks;
  data_callbacks_t error_callbacks;

  std::function<void()> disconnect_callback;

  void receive_to_rx_buf() {
    size_t avail;

    while ((avail = Serial.available()) > 0) {
      if (rx_buffer.size() + avail > rx_buffer.capacity()) {
        // Extend rx buffer if needed
        rx_buffer.reserve(rx_buffer.size() + avail);
      }

      // Use low-level API to read all bytes
      rx_buffer.resize(rx_buffer.size() + avail);

      const size_t offset = rx_buffer.size() - avail;

      tud_task();
      tud_cdc_read(rx_buffer.data() + offset, avail);
    }
  }

  std::optional<ReservedErrorCode> receive_host_hello(Packet &packet) {
    auto &payload = packet.payload();

    const PacketDecoder decoder{payload};

    if (payload.size() < 3)
      return ReservedErrorCode::MalformedPacket;

    const auto protocol_version = decoder.pop_uint16();

    if (protocol_version != VERSION)
      return ReservedErrorCode::UnsupportedProtocolVersion;

    const auto capability_count = decoder.pop_uint8();

    if (payload.size() < capability_count * (2 + 2))
      return ReservedErrorCode::MalformedPacket;

    for (size_t i = 0; i < capability_count; ++i) {
      const auto cap_type = decoder.pop_uint16();
      const auto cap_size = decoder.pop_uint16();

      if (payload.size() < cap_size)
        return ReservedErrorCode::MalformedPacket;

      auto cap_data = decoder.pop_bytes(cap_size);

      const auto cap_it = std::find_if(
        host_capabilities.cbegin(), host_capabilities.cend(),
        [cap_type](const auto &cap) { return cap.get().id() == cap_type; }
      );

      if (cap_it == host_capabilities.cend())
        return ReservedErrorCode::MissingCapabilities;

      auto &cap = cap_it->get();

      if (cap.min_size() > cap_size)
        return ReservedErrorCode::MalformedPacket;

      if (!cap.deserialize(PacketDecoder{cap_data}))
        return ReservedErrorCode::MalformedPacket;
    }

    return std::nullopt;
  }

  void send_device_hello() const {
    std::vector<std::byte> payload;

    {
      const PacketEncoder encoder{payload};

      const auto device_id = device_id::get();

      encoder.push_number(device_id);
      encoder.push_number(static_cast<uint8_t>(device_capabilities.size()));

      for (auto it = device_capabilities.begin(); it != device_capabilities.end(); ++it) {
        const auto &cap = it->get();

        std::vector<std::byte> cap_data;
        PacketEncoder cap_encoder{cap_data};

        cap.serialize(cap_encoder);

        encoder.push_number(cap.id());
        encoder.push_number(static_cast<uint16_t>(cap_data.size()));
        encoder.push_bytes(cap_data.data(), cap_data.size());
      }
    }

    const Packet packet{static_cast<uint16_t>(PacketType::DeviceHello),
                        std::move(payload)};

    send_packet(packet);
  }

  // Returns true on success
  bool process_handshake() {
    assert(rx_packet.has_value());
    assert(current_handshake != HandshakeStage::Completed);

    auto &packet = rx_packet.value();

    if (current_handshake == HandshakeStage::None) {
      if (packet.type() != static_cast<uint16_t>(PacketType::HostHello))
        return true; // Ignore non-handshake packets while not connected

      // Process HostHello
      if (const auto error = receive_host_hello(packet)) {
        send_error(static_cast<uint16_t>(error.value()));

        return false;
      }

      current_handshake = HandshakeStage::HostHelloReceived;

      // Send DeviceHello
      send_device_hello();

      current_handshake = HandshakeStage::DeviceHelloSent;

      return true;
    }

    if (current_handshake == HandshakeStage::DeviceHelloSent) {
      if (packet.type() != static_cast<uint16_t>(PacketType::HostAck))
        return false;

      current_handshake = HandshakeStage::Completed;

      return true;
    }

    return false;
  }
};

#pragma endregion /* Packet Communicator Implementation */

PacketCommunicator comm;

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

  void serialize(const PacketEncoder &encoder) const override {
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

  bool deserialize(const PacketDecoder &decoder) override {
    if (decoder.remaining() < (4 + 4 + 4))
      return false;

    frequency = decoder.pop_float();
    volume = decoder.pop_float();

    const auto raw_duration = decoder.pop_uint32();

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

  bool deserialize(const PacketDecoder &decoder) override {
    if (decoder.remaining() < (1 + 1 + 1))
      return false;

    r = decoder.pop_uint8();
    g = decoder.pop_uint8();
    b = decoder.pop_uint8();

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

void process_data(const uint8_t flags, const PacketDecoder &decoder) {
  // 0 bit are currently unused

  if (get_n_bit(flags, 1)) {
    // Change waveform

    if (decoder.remaining() < 2) {
      comm.send_error(
          static_cast<uint16_t>(ReservedErrorCode::MalformedPacket));

      return;
    }

    rp2040.fifo.push_nb(FIFO_WAVEFORM);
    rp2040.fifo.push_nb(static_cast<uint32_t>(decoder.pop_uint16()));
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

    const auto led_on = decoder.pop_uint8() > 0;

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

class FraiselaitDeviceCapability : public capability::ICapability {
public:
  FraiselaitDeviceCapability() = default;

  [[nodiscard]] uint16_t id() const override { return 0x0040; }

  [[nodiscard]] uint16_t min_size() const override { return 0; }

  void serialize(const PacketEncoder &encoder) const override {
  }

  bool deserialize(const PacketDecoder &encoder) override {
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
    [](std::vector<std::byte> &) { send_data(); }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataGetLoopOff),
    [](std::vector<std::byte> &) { send_data_forever = false; }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataGetLoopOn),
    [](std::vector<std::byte> &) { send_data_forever = true; }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataSet),
    [](std::vector<std::byte> &payload) {
      if (payload.empty()) {
        comm.send_error(static_cast<uint16_t>(
            ReservedErrorCode::MalformedPacket));

        return;
      }

      const PacketDecoder decoder{payload};
      const auto flags = decoder.pop_uint8();

      process_data(flags, decoder);
    }
  );

  comm.on_disconnect(reset_state);
}

void loop() {
  comm.loop();

  if (comm.is_waiting_connection())
    wait_for_serial();

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
