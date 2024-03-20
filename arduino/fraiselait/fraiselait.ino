/*
 * Open Source Licenses:
 * ArduinoJson: MIT License, (c) 2014-2024 Benoît Blanchon.
 */

/* I/O */

#define SPEAKER_PIN 16
#define SWITCH_PIN 18
#define GREEN_PIN 19
#define BLUE_PIN 20
#define RED_PIN 21
#define LIGHT_SENSOR_PIN 26
#define ANALOG_READINGS 24

#include <ArduinoJson.h>

using namespace std;

/* DESERIALIZE */

// Bitflags
typedef union Commands {
  unsigned long all;
  struct {
    unsigned short change_color:1;
    unsigned short change_led_builtin:1;
    unsigned short tone:1;
    unsigned short no_tone:1;
  };
} Commands_t;

typedef struct ChangeColor {
  uint8_t r;
  uint8_t g;
  uint8_t b;
} ChangeColor_t;

typedef struct Tone {
  unsigned int frequency;
  unsigned long *duration;
} Tone_t;

int light_strength_readings[ANALOG_READINGS];
int light_strength_read_index = 0;
int light_strength_total = 0;
int light_strength_average = 0;

float core_temp_readings[ANALOG_READINGS];
int core_temp_read_index = 0;
float core_temp_total = 0;
float core_temp_average = 0;

void smooth_analog_values() {
  light_strength_total -= light_strength_readings[light_strength_read_index];
  light_strength_readings[light_strength_read_index] = analogRead(LIGHT_SENSOR_PIN);
  light_strength_total += light_strength_readings[light_strength_read_index];
  light_strength_read_index++;

  if (light_strength_read_index >= ANALOG_READINGS) {
    light_strength_read_index = 0;
  }

  light_strength_average = light_strength_total / ANALOG_READINGS;

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
  auto button_pressing = digitalRead(SWITCH_PIN) == LOW;

  JsonDocument doc;

  doc["button_pressing"] = button_pressing;
  doc["light_strength"] = light_strength_average;
  doc["temperature"] = core_temp_average;

  serializeJson(doc, Serial);

  Serial.println();
}

Commands_t current_command;
ChangeColor_t command_change_color_data;
bool command_change_led_builtin_data;
Tone_t command_tone_data;

void command_change_color(ChangeColor_t data) {
  analogWrite(RED_PIN, data.r);
  analogWrite(GREEN_PIN, data.g);
  analogWrite(BLUE_PIN, data.b);
}

void command_change_led_builtin(bool data) {
  digitalWrite(LED_BUILTIN, data);
}

void command_tone(Tone_t data) {
  if (data.duration == nullptr) {
    tone(SPEAKER_PIN, data.frequency);
  } else {
    tone(SPEAKER_PIN, data.frequency, *data.duration);
  }
}

void command_no_tone() {
  noTone(SPEAKER_PIN);
}

bool read_command() {
  unsigned long command = Serial.parseInt(SKIP_WHITESPACE);

  Serial.flush();

  if (Serial.available() == 0) return false;

  if (command < 0) return false;

  current_command.all = command;

  JsonDocument doc;

  DeserializationError error = deserializeJson(doc, Serial);

  if (error) {
    Serial.print(F("deserialization failed: "));

    Serial.println(error.f_str());

    return false;
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
    unsigned int frequency = doc["tone"]["frequency"];
    unsigned long duration = doc["tone"]["duration"];

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

  while (!Serial) continue;

  for (auto i = 0; i < ANALOG_READINGS; i++) {
    light_strength_readings[i] = 0;
    core_temp_readings[i] = 0.0;
  }

  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(SPEAKER_PIN, OUTPUT);
  pinMode(SWITCH_PIN, INPUT_PULLUP);
  pinMode(GREEN_PIN, OUTPUT);
  pinMode(BLUE_PIN, OUTPUT);
  pinMode(RED_PIN, OUTPUT);
  pinMode(LIGHT_SENSOR_PIN, INPUT);
}

void loop() {
  smooth_analog_values();
  send_data();
}

void serialEvent() {
  if (!read_command()) {
    return;
  }
}
