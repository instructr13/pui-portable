#define ENABLE_DEBUG_LOG false

#include <Arduino.h>

#include <array>
#include <functional>
#include <optional>

#define PCOMM_ENABLE_DEBUG_LOG ENABLE_DEBUG_LOG

#include "SerialCommunicator.h"
#include "constants.h"
#include "xxh32.h"

#include <ToneDynamic/Speaker.h>

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

  void serialize(const pcomm::bytes::Encoder &encoder) const override {
    encoder.push_bool(button_pressing);
    encoder.push_number(light_strength_average);
    encoder.push_number(core_temp_average);
  }
};

struct ToneData final : IDeserializable {
  float frequency{};
  float volume = 1;
  std::optional<uint32_t> duration;

  ToneData() = default;

  explicit ToneData(const float frequency, const float volume = 1,
       const std::optional<uint32_t> duration = std::nullopt)
      : frequency(frequency), volume(volume), duration(duration) {}

  bool deserialize(pcomm::bytes::Decoder &decoder) override {
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

  bool deserialize(pcomm::bytes::Decoder &decoder) override {
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

void process_data(const uint8_t flags, pcomm::bytes::Decoder &decoder) {
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

class FraiselaitDeviceCapability final : public ICapability {
public:
  FraiselaitDeviceCapability() = default;

  [[nodiscard]] uint16_t id() const override { return 0x0040; }

  [[nodiscard]] uint16_t min_size() const override { return 0; }

  void serialize(const pcomm::bytes::Encoder &encoder) const override {
  }

  bool deserialize(pcomm::bytes::Decoder &encoder) override {
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
  rp2040.fifo.push_nb(FIFO_WAVEFORM);
  rp2040.fifo.push_nb(static_cast<uint32_t>(WaveformType::Square));
}

void setup() {
  Serial.begin(115200);

  wait_for_serial();

  comm.add_capability(fraiselaitDeviceCap, fraiselaitDeviceCap);

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataGetImmediate),
    [](const auto &) { send_data(); }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataGetLoopOff),
    [](const auto &) { send_data_forever = false; }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataGetLoopOn),
    [](const auto &) { send_data_forever = true; }
  );

  comm.subscribe_data(
    static_cast<uint16_t>(DataTypes::CommandDataSet),
    [](std::vector<uint8_t> payload) {
      if (payload.empty()) {
        comm.send_error(static_cast<uint16_t>(
            ReservedErrorCode::MalformedPacket));

        return;
      }

      pcomm::bytes::Decoder decoder{payload.data(), payload.size()};
      const auto flags = decoder.pop_number<uint8_t>();

      process_data(flags, decoder);
    }
  );

  comm.on_disconnect([] {
    reset_state();
    wait_for_serial();
  });
}

void loop() {
  comm.update();

  if (!comm.is_connected())
    return;

  if (send_data_forever)
    send_data();
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

static tone_dynamic::Speaker sp{PIN_SPEAKER, true};

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

void command_change_waveform(const WaveformType type) {
  switch (type) {
    case WaveformType::Square:
      sp.set_waveform(tone_dynamic::SQUARE_WAVEFORM);

      break;

    case WaveformType::Square25:
      sp.set_waveform(tone_dynamic::SQUARE_25_WAVEFORM);

      break;

    case WaveformType::Square12:
      sp.set_waveform(tone_dynamic::SQUARE_12_WAVEFORM);

      break;

    case WaveformType::Triangle:
      sp.set_waveform(tone_dynamic::TRIANGLE_WAVEFORM);

      break;

    case WaveformType::Saw:
      sp.set_waveform(tone_dynamic::SAW_WAVEFORM);

      break;

    case WaveformType::Sine:
      sp.set_waveform(tone_dynamic::SINE_WAVEFORM);

      break;

    case WaveformType::Noise:
      sp.set_waveform(tone_dynamic::NOISE_WAVEFORM);

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
      command_tone(*reinterpret_cast<const ToneData *>(rp2040.fifo.pop()));
    } else if (cmd == FIFO_LED_BUILTIN) {
      command_change_led_builtin(rp2040.fifo.pop());
    } else if (cmd == FIFO_RGB_LED) {
      command_change_color(*reinterpret_cast<const RGBColorData *>(rp2040.fifo.pop()));
    } else if (cmd == FIFO_WAVEFORM) {
      command_change_waveform(static_cast<WaveformType>(rp2040.fifo.pop()));
    }
  }

  smooth_analog_values();
  button_pressing = digitalRead(PIN_TACT_SWITCH) == LOW;
}
