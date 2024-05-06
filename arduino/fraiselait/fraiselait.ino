/*
 * Open Source Licenses:
 * ArduinoJson: MIT License, (c) 2014-2024 Benoît Blanchon.
 * ArduinoUniqueID: MIT License, (c) 2019 Luiz H. Cassettari
 */
#define PROTOCOL_VERSION 200
#define ACK_TIMEOUT_MS 5000

/* I/O */

// See PinInformation struct for details of the default pins
#define PINS_DEFAULT { 16, 18, 19, 20, 21, 26 }

/* COMMANDS */

#define MAGIC_COMMAND_NEGOTIATE 0xCAFEFACE

/* SENSORS */

#define ANALOG_READINGS 24

/* LIBRARIES */

#include <ArduinoJson.h>
#include <ArduinoUniqueID.h>

using namespace std;

/* SERIALIZE */

typedef struct {
  uint8_t code;
  String message;
} Error;

typedef enum {
  TRANSFER_JSON, // For debugging, code is 'J', default
  TRANSFER_MSGPACK, // For real communication, code is 'M'
} TransferMode;

/* DESERIALIZE */

// See PINS_DEFAULT for default pins
typedef struct {
  uint8_t speaker;
  uint8_t tact_switch;
  uint8_t led_green;
  uint8_t led_blue;
  uint8_t led_red;
  uint8_t light_sensor;
} PinInformation;

// Bitflags
typedef union {
  unsigned long all;
  struct {
    unsigned short change_color:1;
    unsigned short change_led_builtin:1;
    unsigned short tone:1;
    unsigned short no_tone:1;
    unsigned short change_pin:1;
    unsigned short restore_default_pins:1;
  };
} Commands;

typedef struct {
  uint8_t r;
  uint8_t g;
  uint8_t b;
} ChangeColor;

typedef struct {
  uint16_t frequency;
  uint32_t *duration;
} Tone;

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

char* get_device_id() {
  static char id[10];
  static bool output_hash_got = false;

  if (!output_hash_got) {
    auto hash = XXH32((char*) UniqueID, UniqueIDsize, 0);

    sprintf(id, "D%09x", id);

    output_hash_got = true;
  }

  return &id[0];
}

bool negotiated = false;

// Default pin values
PinInformation pins = PINS_DEFAULT;

// Default transfer mode
TransferMode transfer_mode = TRANSFER_JSON;

void send_error(Error e) {
  JsonDocument doc;

  doc["code"] = e.code;
  doc["message"] = e.message;

  Serial.print(F("E"));
  serialize(doc);
  Serial.println();
}

size_t serialize(const JsonDocument &doc) {
  size_t size;

  switch (transfer_mode) {
    case TRANSFER_JSON:
    size = serializeJson(doc, Serial);

    break;

    case TRANSFER_MSGPACK:
    size = serializeMsgPack(doc, Serial);

    break;
  }

  return size;
}

bool deserialize(JsonDocument &doc) {
  DeserializationError error;

  switch (transfer_mode) {
    case TRANSFER_JSON:
    error = deserializeJson(doc, Serial);

    break;

    case TRANSFER_MSGPACK:
    error = deserializeMsgPack(doc, Serial);

    break;
  }

  if (error) {
    String message = F("Deserialization failed: ");

    message.concat(error.f_str());
    send_error({ 1, message });

    return false;
  }

  return true;
}

void refresh_pins() {
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(pins.speaker, OUTPUT);
  pinMode(pins.tact_switch, INPUT_PULLUP);
  pinMode(pins.led_green, OUTPUT);
  pinMode(pins.led_blue, OUTPUT);
  pinMode(pins.led_red, OUTPUT);
  pinMode(pins.light_sensor, INPUT);
}

int light_strength_average = 0;
int light_strength_readings[ANALOG_READINGS];
float core_temp_average = 0;
float core_temp_readings[ANALOG_READINGS];

void smooth_analog_values() {
  static int light_strength_read_index = 0;
  static int light_strength_total = 0;

  light_strength_total -= light_strength_readings[light_strength_read_index];
  light_strength_readings[light_strength_read_index] = analogRead(pins.light_sensor);
  light_strength_total += light_strength_readings[light_strength_read_index];
  light_strength_read_index++;

  if (light_strength_read_index >= ANALOG_READINGS) {
    light_strength_read_index = 0;
  }

  light_strength_average = light_strength_total / ANALOG_READINGS;

  static int core_temp_read_index = 0;
  static float core_temp_total = 0;

  core_temp_total -= core_temp_readings[core_temp_read_index];
  core_temp_readings[core_temp_read_index] = analogReadTemp();
  core_temp_total += core_temp_readings[core_temp_read_index];
  core_temp_read_index++;

  if (core_temp_read_index >= ANALOG_READINGS) {
    core_temp_read_index = 0;
  }

  core_temp_average = core_temp_total / ANALOG_READINGS;
}

void send_data() {
  auto button_pressing = digitalRead(pins.tact_switch) == LOW;

  JsonDocument doc;

  doc["button_pressing"] = button_pressing;
  doc["light_strength"] = light_strength_average;
  doc["temperature"] = core_temp_average;

  serialize(doc);

  Serial.println();
}

Commands current_command;
ChangeColor command_change_color_data;
bool command_change_led_builtin_data;
Tone command_tone_data;

void command_change_color(ChangeColor data) {
  analogWrite(pins.led_red, data.r);
  analogWrite(pins.led_green, data.g);
  analogWrite(pins.led_blue, data.b);
}

void command_change_led_builtin(bool data) {
  digitalWrite(LED_BUILTIN, data);
}

void command_tone(Tone data) {
  if (data.duration == nullptr) {
    tone(pins.speaker, data.frequency);
  } else {
    tone(pins.speaker, data.frequency, *data.duration);
  }
}

void command_no_tone() {
  noTone(pins.speaker);
}

void change_pins(PinInformation new_pins) {
  bool need_to_restore_tone = false;

  if (pins.speaker != new_pins.speaker) {
    if (command_tone_data.duration == nullptr) {
      need_to_restore_tone = true;
    }

    command_no_tone();
  }

  bool led_red_changed = pins.led_red != new_pins.led_red;
  bool led_green_changed = pins.led_green != new_pins.led_green;
  bool led_blue_changed = pins.led_blue != new_pins.led_blue;

  bool need_to_restore_color = led_red_changed || led_green_changed || led_blue_changed;

  uint8_t r, g, b;

  if (led_red_changed) {
    r = command_change_color_data.r;

    command_change_color_data.r = 0;
  }

  if (led_green_changed) {
    g = command_change_color_data.g;

    command_change_color_data.g = 0;
  }

  if (led_blue_changed) {
    b = command_change_color_data.b;

    command_change_color_data.b = 0;
  }

  if (need_to_restore_color) {
    command_change_color(command_change_color_data);
  }

  pins = new_pins;

  refresh_pins();

  if (need_to_restore_tone) {
    command_tone(command_tone_data);
  }

  if (need_to_restore_color) {
    command_change_color_data = {
      led_red_changed   ? r : command_change_color_data.r,
      led_green_changed ? g : command_change_color_data.g,
      led_blue_changed  ? b : command_change_color_data.b,
    };

    command_change_color(command_change_color_data);
  }
}

void command_change_pin(JsonDocument &doc) {
  static const __FlashStringHelper* pin_names[6] = {
    F("speaker"),
    F("tact switch"),
    F("green led"),
    F("blue led"),
    F("red led"),
    F("light sensor"),
  };

  uint8_t speaker_pin = doc["pins"]["speaker"] | pins.speaker;
  uint8_t tact_switch_pin = doc["pins"]["tact_switch"] | pins.tact_switch;
  uint8_t led_green_pin = doc["pins"]["led_green"] | pins.led_green;
  uint8_t led_blue_pin = doc["pins"]["led_blue"] | pins.led_blue;
  uint8_t led_red_pin = doc["pins"]["led_red"] | pins.led_red;
  uint8_t light_sensor_pin = doc["pins"]["light_sensor"] | pins.light_sensor;

  {
    uint8_t pins[6] = {
      speaker_pin,
      tact_switch_pin,
      led_green_pin,
      led_blue_pin,
      led_red_pin,
      light_sensor_pin,
    };

    // Check for pin duplicates
    for (auto i = 0; i < 6; i++) {
      for (auto j = 0; j < 6; j++) {
        if (i == j) continue;

        if (pins[i] == pins[j]) {
          String message = F("Pin collision detected: ");

          message.concat(pin_names[i]);
          message.concat(F(" pin and "));
          message.concat(pin_names[j]);
          message.concat(F(" pin are the same number"));

          send_error({ 16, message });

          return;
        }
      }
    }
  }

  change_pins({
    speaker_pin,
    tact_switch_pin,
    led_green_pin,
    led_blue_pin,
    led_red_pin,
    light_sensor_pin
  });
}

void command_restore_default_pins() {
  change_pins(PINS_DEFAULT);
}

JsonDocument make_device_info() {
  JsonDocument doc;

  doc["version"] = PROTOCOL_VERSION;
  doc["device_id"] = get_device_id();

  return doc;
}

bool negotiate() {
  char desired_transfer_mode = Serial.peek();

  if (desired_transfer_mode == -1) {
    send_error({ 2, F("Negotiation failed: No data received") });

    return false;
  }

  if (desired_transfer_mode != '{') {
    char null_buf;

    Serial.readBytes(&null_buf, 1);

    switch (desired_transfer_mode) {
      case 'J':
      transfer_mode = TRANSFER_JSON;

      break;

      case 'M':
      transfer_mode = TRANSFER_MSGPACK;

      break;

      default: {
        String message_ = F("Negotiation failed: Invalid transfer mode: ");

        send_error({ 2, message_ + desired_transfer_mode });

        return false;
      }
    }
  }

  JsonDocument doc;

  if (!deserialize(doc)) return false;

  uint16_t version = doc["version"];

  if (version < PROTOCOL_VERSION) {
    String message = F("Negotiation failed: Incompatible protocol version: ");

    message.concat(F("Expected minimal version is "));
    message.concat(PROTOCOL_VERSION);
    message.concat(F(", got "));
    message.concat(version);

    send_error({ 2, message });

    return false;
  }

  command_change_pin(doc);

  serialize(make_device_info());
  Serial.println();

  char host_answer;
  auto timeout_start = millis();

  Serial.readBytes(&host_answer, 1);

  while (host_answer == -1 || host_answer == '\n') {
    if (millis() - timeout_start > ACK_TIMEOUT_MS) {
      send_error({ 2, F("Negotiation failed: Host answer timed out") });

      return false;
    }

    if (Serial.available() > 0) {
      Serial.readBytes(&host_answer, 1);
    }
  }

  if (host_answer == 'E') { // means that host reported an error
    if (!deserialize(doc)) return false;

    String message = F("Negotiation failed: ");

    message.concat(F("Host sent error with code "));
    uint8_t code = doc["code"];
    message.concat(code);
    message.concat(F(": "));
    String message_ = doc["message"] | "[Empty message]";
    message.concat(message_);

    send_error({ 2, message });

    return false;
  }

  if (host_answer == 'A') { // means acknowledgements
    negotiated = true;

    return true;
  }

  String message_ = F("Negotiation failed: invalid host answer received: ");

  send_error({ 2, message_ + host_answer });

  return false;
}

bool read_command() {
  Serial.flush();

  unsigned long command = Serial.parseInt(SKIP_WHITESPACE);

  Serial.flush();

  if (Serial.available() == 0 || command < 0) return false;

  if (!negotiated) {
    if (command != MAGIC_COMMAND_NEGOTIATE) {
      send_error({ 0, F("Need to negotiate") });

      Serial.readStringUntil('\n');

      return false;
    }

    return negotiate();
  }

  char* device_id_buf;

  Serial.readBytes(device_id_buf, 10);

  if (strcmp(get_device_id(), device_id_buf) != 0) return false;

  Serial.flush();

  current_command.all = command;

  JsonDocument doc;

  if (!deserialize(doc)) return false;

  if (current_command.change_pin) {
    command_no_tone();

    command_change_pin(doc);
  } else if (current_command.restore_default_pins) {
    command_no_tone();

    command_restore_default_pins();
  }

  if (current_command.change_color) {
    uint8_t r = doc["color"]["r"];
    uint8_t g = doc["color"]["g"];
    uint8_t b = doc["color"]["b"];

    command_change_color_data = { r, g, b };

    command_change_color(command_change_color_data);
  }

  if (current_command.change_led_builtin) {
    command_change_led_builtin_data = doc["led_builtin"];

    command_change_led_builtin(command_change_led_builtin_data);
  }

  if (current_command.tone) {
    uint16_t frequency = doc["tone"]["frequency"];
    uint32_t duration = doc["tone"]["duration"];

    command_tone_data = { frequency, &duration };

    command_tone(command_tone_data);
  } else if (current_command.no_tone) {
    command_no_tone();
  }

  return true;
}

void setup() {
  Serial.begin(115200);
  Serial.setTimeout(16);

  while (!Serial);

  for (auto i = 0; i < ANALOG_READINGS; i++) {
    light_strength_readings[i] = 0;
    core_temp_readings[i] = 0.0;
  }

  refresh_pins();
}

void loop() {
  smooth_analog_values();

  if (!negotiated) return;

  send_data();
}

void serialEvent() {
  if (!read_command()) return;
}
