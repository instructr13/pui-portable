#pragma once

#include <functional>

#include <pcomm/pcomm.h>

enum class DataTypes : uint16_t {
  CommandDataGetImmediate = 0x0090,
  CommandDataGetLoopOff = 0x0092,
  CommandDataGetLoopOn = 0x0093,
  CommandDataSet = 0x00e0,

  ResponseDataSend = 0x00f0,
};

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

  virtual void serialize(const pcomm::bytes::Encoder &encoder) const = 0;
};

class IDeserializable {
public:
  virtual ~IDeserializable() = default;

  virtual bool deserialize(pcomm::bytes::Decoder &decoder) = 0;
};

class ICapability : public ISerializable, public IDeserializable {
public:
  [[nodiscard]] virtual uint16_t id() const = 0;

  [[nodiscard]] virtual uint16_t min_size() const = 0;
};

using DataCallback = std::function<void(std::vector<uint8_t> data)>;

using data_callback_pair_t = std::pair<uint16_t, DataCallback>;
using data_callbacks_t = std::vector<data_callback_pair_t>;

class SerialCommunicator final : public pcomm::socket::SerialUSBSocket {
public:
  static constexpr uint16_t VERSION = 410;

  [[nodiscard]] bool is_connected() const {
    return current_handshake_stage == HandshakeStage::Completed;
  }

  void on_unavailable() override;

  void on_recv(pcomm::packets::Packet packet) override;

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

  void send_data(uint16_t code, const std::vector<uint8_t> &data_payload = {});

  void send_data(uint16_t code, const ISerializable &data);

  void send_error(uint16_t code,
                  const std::vector<uint8_t> &error_payload = {});

  void send_error(uint16_t code, const ISerializable &data);

private:
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
  find_data_callback(uint16_t type, data_callbacks_t &callbacks);

  static void add_data_callback(uint16_t type, DataCallback &&callback,
                                data_callbacks_t &callbacks);

  [[nodiscard]] std::optional<ReservedErrorCode>
  process_host_hello(const pcomm::packets::Packet &packet) const;

  void send_device_hello();

  bool process_handshake(PacketType type, const pcomm::packets::Packet &packet);
};
