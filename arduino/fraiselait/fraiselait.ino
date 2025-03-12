/* LIBRARIES */

#include <array>
#include <optional>

#include "hardware/flash.h"

using namespace std;

///bool core1_separate_stack = true;

constexpr uint16_t PROTOCOL_VERSION = 300;
constexpr uint32_t ACK_TIMEOUT_MS = 5000;

/* COMMANDS */

constexpr uint8_t COMMAND_DATA_GET_IMMEDIATE = 0x90;
constexpr uint8_t COMMAND_DATA_GET_LOOP_OFF = 0x92;
constexpr uint8_t COMMAND_DATA_GET_LOOP_ON = 0x93;

constexpr uint8_t COMMAND_DATA_SET = 0xe0;

constexpr uint8_t COMMAND_DEVICE_INFO_GET = 0xf0;

/* RESPONSE */

constexpr uint8_t RESPONSE_DATA_START = 0x9f;
constexpr uint8_t RESPONSE_RESERVED_ERROR = 0xfe;

/* FIFO COMMANDS */

constexpr uint32_t FIFO_REFRESH_PINS = 0xcafe000d;
constexpr uint32_t FIFO_NO_TONE = 0xcafe0000;
constexpr uint32_t FIFO_TONE = 0xcafe000a;
constexpr uint32_t FIFO_RGB_LED = 0xcafe000b;
constexpr uint32_t FIFO_LED_BUILTIN = 0xcafe000c;

/* SENSORS */

constexpr size_t ANALOG_READINGS = 24;

constexpr uint32_t CUSTOM_DEVICE_ID = 0;

struct PinInformation {
  // Default pins
  uint8_t speaker = 16;
  uint8_t tact_switch = 18;
  uint8_t led_green = 19;
  uint8_t led_blue = 20;
  uint8_t led_red = 21;
  uint8_t light_sensor = 26;
};

struct OptionalPinInformation {
  optional<uint8_t> speaker;
  optional<uint8_t> tact_switch;
  optional<uint8_t> led_green;
  optional<uint8_t> led_blue;
  optional<uint8_t> led_red;
  optional<uint8_t> light_sensor;

  PinInformation merge(const PinInformation &pins) const {
    return {
      speaker.value_or(pins.speaker),
      tact_switch.value_or(pins.tact_switch),
      led_green.value_or(pins.led_green),
      led_blue.value_or(pins.led_blue),
      led_red.value_or(pins.led_red),
      light_sensor.value_or(pins.light_sensor)
    };
  }
};

struct Tone {
  uint16_t frequency;
  optional<uint32_t> duration;

  Tone() {}
  Tone(const uint16_t frequency, const optional<uint32_t> duration = nullopt) : frequency(frequency), duration(duration) {}
};

struct RGBColor {
  uint8_t r;
  uint8_t g;
  uint8_t b;
};

struct DeserializationError {
  void send() const {
    Serial.write(RESPONSE_RESERVED_ERROR);
    Serial.write(0x01);
  }
};

struct PinCollisionError {
  void send() const {
    Serial.write(RESPONSE_RESERVED_ERROR);
    Serial.write(0x02);
  }
};

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

uint32_t XXH32(void const *const input, size_t const length, uint32_t const seed);

static uint32_t const PRIME32_1 = 0x9E3779B1U;   /* 0b10011110001101110111100110110001 */
static uint32_t const PRIME32_2 = 0x85EBCA77U;   /* 0b10000101111010111100101001110111 */
static uint32_t const PRIME32_3 = 0xC2B2AE3DU;   /* 0b11000010101100101010111000111101 */
static uint32_t const PRIME32_4 = 0x27D4EB2FU;   /* 0b00100111110101001110101100101111 */
static uint32_t const PRIME32_5 = 0x165667B1U;   /* 0b00010110010101100110011110110001 */

/* Rotates value left by amt. */
static uint32_t XXH_rotl32(uint32_t const value, uint32_t const amt)
{
  return (value << (amt % 32)) | (value >> (32 - (amt % 32)));
}

/* Portably reads a 32-bit little endian integer from data at the given offset. */
static uint32_t XXH_read32(uint8_t const *const data, size_t const offset)
{
  return (uint32_t) data[offset + 0]
    | ((uint32_t) data[offset + 1] << 8)
    | ((uint32_t) data[offset + 2] << 16)
    | ((uint32_t) data[offset + 3] << 24);
}

/* Mixes input into acc. */
static uint32_t XXH32_round(uint32_t acc, uint32_t const input)
{
  acc += input * PRIME32_2;
  acc  = XXH_rotl32(acc, 13);
  acc *= PRIME32_1;
  return acc;
}

/* Mixes all bits to finalize the hash. */
static uint32_t XXH32_avalanche(uint32_t hash)
{
  hash ^= hash >> 15;
  hash *= PRIME32_2;
  hash ^= hash >> 13;
  hash *= PRIME32_3;
  hash ^= hash >> 16;
  return hash;
}

/* The XXH32 hash function.
 * input:   The data to hash.
 * length:  The length of input. It is undefined behavior to have length larger than the
 *          capacity of input.
 * seed:    A 32-bit value to seed the hash with.
 * returns: The 32-bit calculated hash value. */
uint32_t XXH32(void const *const input, size_t const length, uint32_t const seed)
{
  uint8_t const *const data = (uint8_t const *) input;
  uint32_t hash;
  size_t remaining = length;
  size_t offset = 0;

  /* Don't dereference a null pointer. The reference implementation notably doesn't
   * check for this by default. */
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
      acc1 = XXH32_round(acc1, XXH_read32(data, offset)); offset += 4;
      acc2 = XXH32_round(acc2, XXH_read32(data, offset)); offset += 4;
      acc3 = XXH32_round(acc3, XXH_read32(data, offset)); offset += 4;
      acc4 = XXH32_round(acc4, XXH_read32(data, offset)); offset += 4;
      remaining -= 16;
    }

    hash = XXH_rotl32(acc1, 1) + XXH_rotl32(acc2, 7) + XXH_rotl32(acc3, 12) + XXH_rotl32(acc4, 18);
  } else {
    /* Not enough data for the main loop, put something in there instead. */
    hash = seed + PRIME32_5;
  }

  hash += (uint32_t) length;

  /* Process the remaining data. */
  while (remaining >= 4) {
    hash += XXH_read32(data, offset) * PRIME32_3;
    hash  = XXH_rotl32(hash, 17);
    hash *= PRIME32_4;
    offset += 4;
    remaining -= 4;
  }

  while (remaining != 0) {
    hash += (uint32_t) data[offset] * PRIME32_5;
    hash  = XXH_rotl32(hash, 11);
    hash *= PRIME32_1;
    --remaining;
    ++offset;
  }
  return XXH32_avalanche(hash);
}

#pragma endregion /* XXH32 Implementation */

uint32_t get_device_id() {
  if constexpr (CUSTOM_DEVICE_ID > 0) {
    return CUSTOM_DEVICE_ID;
  } else {
    static uint32_t id;
    static bool output_hash_got = false;

    if (!output_hash_got) {
      uint8_t raw_id[4];

      flash_get_unique_id(raw_id);

      id = XXH32(raw_id, 4, 0);

      output_hash_got = true;
    }

    return id;
  }
}

PinInformation core0Pins;
PinInformation core1Pins;

void change_pins(const PinInformation &new_pins) {
  core0Pins = new_pins;

  rp2040.fifo.push_nb(FIFO_REFRESH_PINS);
}

void request_restore_default_pins() {
  change_pins({});
}

void request_change_pin(const OptionalPinInformation &maybe_new_pins) {
  const auto new_pins = maybe_new_pins.merge(core0Pins);
  const auto speaker_pin = new_pins.speaker;
  const auto tact_switch_pin = new_pins.tact_switch;
  const auto led_green_pin = new_pins.led_green;
  const auto led_blue_pin = new_pins.led_blue;
  const auto led_red_pin = new_pins.led_red;
  const auto light_sensor_pin = new_pins.light_sensor;

  {
    const array<uint8_t, 6> pins_array = {
      speaker_pin,
      tact_switch_pin,
      led_green_pin,
      led_blue_pin,
      led_red_pin,
      light_sensor_pin,
    };

    // Check for pin duplicates
    for (auto i = 0; i < pins_array.size(); i++) {
      for (auto j = 0; j < pins_array.size(); j++) {
        if (i == j) continue;

        if (pins_array[i] == pins_array[j]) {
          PinCollisionError().send();

          return;
        }
      }
    }
  }

  change_pins(new_pins);
}

volatile bool button_pressing = false;
volatile int light_strength_average = 0;
volatile float core_temp_average = 0;

void send_data() {
  static_assert(sizeof(int) == 4, "This protocol only supports int with 4 bytes");
  static_assert(sizeof(float) == 4, "This protocol only supports float with 4 bytes");

  Serial.write(RESPONSE_DATA_START);

  Serial.write(button_pressing ? 1 : 0);

  const int raw_light_strength_average = light_strength_average;
  const auto *light_strength_average_ptr = reinterpret_cast<const uint8_t*>(&raw_light_strength_average);

  Serial.write(light_strength_average_ptr[3]);
  Serial.write(light_strength_average_ptr[2]);
  Serial.write(light_strength_average_ptr[1]);
  Serial.write(light_strength_average_ptr[0]);

  const float raw_core_temp_average = core_temp_average;
  const auto *core_temp_average_ptr = reinterpret_cast<const uint8_t*>(&raw_core_temp_average);

  Serial.write(core_temp_average_ptr[3]);
  Serial.write(core_temp_average_ptr[2]);
  Serial.write(core_temp_average_ptr[1]);
  Serial.write(core_temp_average_ptr[0]);
}

void send_device_info() {
  const auto *protocol_version_ptr = reinterpret_cast<const uint8_t*>(&PROTOCOL_VERSION);

  Serial.write(protocol_version_ptr[1]);
  Serial.write(protocol_version_ptr[0]);

  const auto device_id = get_device_id();
  const auto *device_id_ptr = reinterpret_cast<const uint8_t*>(&device_id);

  Serial.write(device_id_ptr[3]);
  Serial.write(device_id_ptr[2]);
  Serial.write(device_id_ptr[1]);
  Serial.write(device_id_ptr[0]);

  Serial.write(core0Pins.speaker);
  Serial.write(core0Pins.tact_switch);
  Serial.write(core0Pins.led_green);
  Serial.write(core0Pins.led_blue);
  Serial.write(core0Pins.led_red);
  Serial.write(core0Pins.light_sensor);
}

bool send_data_forever = false;

void setup() {
  Serial.begin(115200);
  Serial.setTimeout(2);

  while (!Serial);
}

void loop() {
  if (send_data_forever)
    send_data();
}

optional<uint8_t> serial_read_char() {
  const auto data = Serial.read();

  if (data == -1)
    return nullopt;

  return static_cast<uint8_t>(data);
}

template <std::size_t N>
optional<array<uint8_t, N>> serial_read_chars() {
  array<uint8_t, N> ret;

  for (std::size_t i = 0; i < N; ++i) {
    const auto data = serial_read_char();

    if (!data) return nullopt;

    ret[i] = data.value();
  }

  return ret;
}

bool get_n_bit(uint8_t flags, std::size_t n) {
  return (flags >> n) & 0b1;
}

Tone tone_data;
RGBColor change_color_data;

void serialEvent() {
  static_assert(sizeof(void*) == 4, "This protocol only supports 32-bit pointers");

  if (Serial.available() == 0) return;

  const auto cmd = serial_read_char();

  if (!cmd) return;

  if (cmd == COMMAND_DATA_GET_IMMEDIATE) {
    send_data();

    return;
  }
  
  if (cmd == COMMAND_DATA_GET_LOOP_ON) {
    send_data_forever = true;

    return;
  }
  
  if (cmd == COMMAND_DATA_GET_LOOP_OFF) {
    send_data_forever = false;

    return;
  }
  
  if (cmd == COMMAND_DEVICE_INFO_GET) {
    send_device_info();

    return;
  }
  
  if (cmd == COMMAND_DATA_SET) {
    const auto maybe_flags = serial_read_char();

    if (!maybe_flags) {
      DeserializationError().send();

      return;
    }

    const auto flags = maybe_flags.value();

    if (get_n_bit(flags, 0)) { // Restore default pins
      request_restore_default_pins();
    } else if (get_n_bit(flags, 1)) { // Change pins
      const auto speaker = serial_read_char();
      const auto tact_switch = serial_read_char();
      const auto led_green = serial_read_char();
      const auto led_blue = serial_read_char();
      const auto led_red = serial_read_char();
      const auto light_sensor = serial_read_char();

      if (!(speaker && tact_switch && led_green && led_blue && led_red && light_sensor)) {
        DeserializationError().send();

        return;
      }

      request_change_pin({
        speaker == 0 ? nullopt : speaker,
        tact_switch == 0 ? nullopt : tact_switch,
        led_green == 0 ? nullopt : led_green,
        led_blue == 0 ? nullopt : led_blue,
        led_red == 0 ? nullopt : led_red,
        light_sensor == 0 ? nullopt : light_sensor
      });
    }

    if (get_n_bit(flags, 2)) { // No tone
      rp2040.fifo.push_nb(FIFO_NO_TONE);
    } else if (get_n_bit(flags, 3)) { // Tone
      const auto maybe_data = serial_read_chars<2 + 4>(); // freq + duration

      if (!maybe_data) {
        DeserializationError().send();

        return;
      }

      const auto data = maybe_data.value();

      const uint16_t frequency0 = data[0];
      const uint16_t frequency1 = data[1];

      const uint32_t duration0 = data[2];
      const uint32_t duration1 = data[3];
      const uint32_t duration2 = data[4];
      const uint32_t duration3 = data[5];

      tone_data.frequency = (frequency0 << 8) | frequency1;

      auto duration = (duration0 << 24) | (duration1 << 16) | (duration2 << 8) | duration3;

      if (duration > 0)
        tone_data.duration.emplace(std::move(duration));
      else
        tone_data.duration.reset();

      rp2040.fifo.push_nb(FIFO_TONE);
      rp2040.fifo.push_nb(reinterpret_cast<uint32_t>(&tone_data));
    }

    if (get_n_bit(flags, 4)) { // LED Builtin
      const auto maybe_data = serial_read_char();

      if (!maybe_data) {
        DeserializationError().send();

        return;
      }

      const auto data = maybe_data.value();

      rp2040.fifo.push_nb(FIFO_LED_BUILTIN);
      rp2040.fifo.push_nb(data > 0);
    }

    if (get_n_bit(flags, 5)) { // RGB LED
      const auto maybe_data = serial_read_chars<1 + 1 + 1>(); // r + g + b

      if (!maybe_data) {
        DeserializationError().send();

        return;
      }

      const auto data = maybe_data.value();

      change_color_data.r = data[0];
      change_color_data.g = data[1];
      change_color_data.b = data[2];

      rp2040.fifo.push_nb(FIFO_RGB_LED);
      rp2040.fifo.push_nb(reinterpret_cast<uint32_t>(&change_color_data));
    }

    return;
  }
}

void smooth_analog_values() {
  static int light_strength_read_index = 0;
  static int light_strength_total = 0;
  static array<int, ANALOG_READINGS> light_strength_readings {};
  static array<float, ANALOG_READINGS> core_temp_readings {};

  light_strength_total -= light_strength_readings[light_strength_read_index];
  light_strength_readings[light_strength_read_index] = analogRead(core1Pins.light_sensor);
  light_strength_total += light_strength_readings[light_strength_read_index];
  light_strength_read_index++;

  if (light_strength_read_index >= light_strength_readings.size()) {
    light_strength_read_index = 0;
  }

  light_strength_average = light_strength_total / light_strength_readings.size();

  static int core_temp_read_index = 0;
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

void refresh_pins() {
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(core1Pins.speaker, OUTPUT);
  pinMode(core1Pins.tact_switch, INPUT_PULLUP);
  pinMode(core1Pins.led_green, OUTPUT);
  pinMode(core1Pins.led_blue, OUTPUT);
  pinMode(core1Pins.led_red, OUTPUT);
  pinMode(core1Pins.light_sensor, INPUT);
}

void command_no_tone() {
  noTone(core1Pins.speaker);
}

void command_tone(const Tone &data) {
  if (data.duration) {
    tone(core1Pins.speaker, data.frequency, data.duration.value());
  } else {
    tone(core1Pins.speaker, data.frequency);
  }
}

void command_change_color(const RGBColor &data) {
  analogWrite(core1Pins.led_red, data.r);
  analogWrite(core1Pins.led_green, data.g);
  analogWrite(core1Pins.led_blue, data.b);
}

void command_change_led_builtin(const bool data) {
  digitalWrite(LED_BUILTIN, data);
}

void setup1() {
  refresh_pins();
}

void loop1() {
  if (uint32_t cmd; rp2040.fifo.pop_nb(&cmd)) {
    if (cmd == FIFO_REFRESH_PINS) {
      core1Pins = core0Pins;

      refresh_pins();
    } else if (cmd == FIFO_NO_TONE) {
      command_no_tone();
    } else if (cmd == FIFO_TONE) {
      command_tone(*reinterpret_cast<Tone*>(rp2040.fifo.pop()));
    } else if (cmd == FIFO_LED_BUILTIN) {
      command_change_led_builtin(rp2040.fifo.pop());
    } else if (cmd == FIFO_RGB_LED) {
      command_change_color(*reinterpret_cast<RGBColor*>(rp2040.fifo.pop()));
    }
  }

  smooth_analog_values();
  button_pressing = digitalRead(core1Pins.tact_switch) == LOW;
}
