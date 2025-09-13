#include "SerialCommunicator.h"

#include "constants.h"
#include "device_id.h"

#include <algorithm>

void SerialCommunicator::on_unavailable() {
  current_handshake_stage = HandshakeStage::None;

  if constexpr (SOFTWARE_RESET_ON_DISCONNECT) {
    watchdog_reboot(0, SRAM_END, 10);
  }

  if (disconnect_callback) {
    disconnect_callback();
  }
}

void SerialCommunicator::on_recv(const pcomm::packets::Packet packet) {
  const auto type = static_cast<PacketType>(packet.type);

  if (type == PacketType::Error) {
    const auto &payload = packet.payload;

    if (payload.size() < 2) {
      // Malformed error packet
      return;
    }

    pcomm::bytes::Decoder decoder{payload.data(), payload.size()};

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
          static_cast<uint16_t>(ReservedErrorCode::HandshakeNotCompleted));
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

    pcomm::bytes::Decoder decoder{payload.data(), payload.size()};

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
  }

  send_debug("Received unknown packet: " +
             std::to_string(static_cast<uint16_t>(type)));

  send_error(static_cast<uint16_t>(ReservedErrorCode::UnknownPacketType));
}

void SerialCommunicator::send_data(const uint16_t code,
                                   const std::vector<uint8_t> &data_payload) {
  if (!is_connected())
    return;

  std::vector<uint8_t> payload;

  const pcomm::bytes::Encoder encoder{payload};

  encoder.push_number(code);
  encoder.push_bytes(data_payload.data(), data_payload.size());

  send(pcomm::packets::Packet(static_cast<uint16_t>(PacketType::Data),
                              std::move(payload)));
}

void SerialCommunicator::send_data(const uint16_t code,
                                   const ISerializable &data) {
  if (!is_connected())
    return;

  std::vector<uint8_t> payload;

  const pcomm::bytes::Encoder encoder{payload};

  encoder.push_number(code);
  data.serialize(encoder);

  send(pcomm::packets::Packet(static_cast<uint16_t>(PacketType::Data),
                              std::move(payload)));
}
void SerialCommunicator::send_error(const uint16_t code,
                                    const std::vector<uint8_t> &error_payload) {
  std::vector<uint8_t> payload;

  const pcomm::bytes::Encoder encoder{payload};

  encoder.push_number(code);
  encoder.push_bytes(error_payload.data(), error_payload.size());

  send(pcomm::packets::Packet(static_cast<uint16_t>(PacketType::Error),
                              std::move(payload)));
}

void SerialCommunicator::send_error(const uint16_t code,
                                    const ISerializable &data) {
  std::vector<uint8_t> payload;

  const pcomm::bytes::Encoder encoder{payload};

  encoder.push_number(code);
  data.serialize(encoder);

  send(pcomm::packets::Packet(static_cast<uint16_t>(PacketType::Error),
                              std::move(payload)));
}

data_callbacks_t::iterator
SerialCommunicator::find_data_callback(const uint16_t type,
                                       data_callbacks_t &callbacks) {
  const auto it = std::ranges::lower_bound(callbacks, type, {},
                                           &data_callback_pair_t::first);

  if (it == callbacks.end() || it->first != type) {
    return callbacks.end();
  }

  return it;
}

void SerialCommunicator::add_data_callback(const uint16_t type,
                                           DataCallback &&callback,
                                           data_callbacks_t &callbacks) {
  const auto it = find_data_callback(type, callbacks);

  if (it != callbacks.end()) {
    // Replace existing
    it->second = callback;

    return;
  }

  callbacks.emplace_back(type, std::move(callback));

  std::ranges::sort(callbacks, {}, &data_callback_pair_t::first);
}

std::optional<ReservedErrorCode> SerialCommunicator::process_host_hello(
    const pcomm::packets::Packet &packet) const {
  auto &payload = packet.payload;

  if (payload.size() < 3) {
    return ReservedErrorCode::MalformedPacket;
  }

  pcomm::bytes::Decoder decoder(payload.data(), payload.size());

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

    const auto cap_it = std::ranges::find_if(
        host_capabilities,
        [=](const std::reference_wrapper<ICapability> &cap) {
          return cap.get().id() == cap_id;
        });

    if (cap_it == host_capabilities.cend()) {
      return ReservedErrorCode::MissingCapabilities;
    }

    auto &cap = cap_it->get();

    if (cap.min_size() > cap_size) {
      return ReservedErrorCode::MalformedPacket;
    }

    pcomm::bytes::Decoder cap_decoder(cap_data.data(), cap_size);

    if (!cap.deserialize(cap_decoder)) {
      return ReservedErrorCode::MalformedPacket;
    }
  }

  return std::nullopt;
}

void SerialCommunicator::send_device_hello() {
  std::vector<uint8_t> payload;

  const pcomm::bytes::Encoder encoder(payload);

  const auto device_id = device_id::get();

  encoder.push_number(device_id);
  encoder.push_number(static_cast<uint8_t>(device_capabilities.size()));

  for (const auto device_capability : device_capabilities) {
    const auto &cap = device_capability.get();

    std::vector<uint8_t> cap_payload;

    const pcomm::bytes::Encoder cap_encoder(cap_payload);

    cap.serialize(cap_encoder);

    encoder.push_number(cap.id());
    encoder.push_number(static_cast<uint16_t>(cap_payload.size()));
    encoder.push_bytes(cap_payload.data(), cap_payload.size());
  }

  send(pcomm::packets::Packet(static_cast<uint16_t>(PacketType::DeviceHello),
                              std::move(payload)));
}

bool SerialCommunicator::process_handshake(
    const PacketType type, const pcomm::packets::Packet &packet) {
  if (current_handshake_stage == HandshakeStage::None) {
    if (type != PacketType::HostHello)
      return true; // Ignore non-handshake packets while not connected

    send_debug("Host hello received");

    if (const auto error = process_host_hello(packet)) {
      send_error(static_cast<uint16_t>(error.value()));

      return false;
    }

    current_handshake_stage = HandshakeStage::HostHelloReceived;

    send_debug("Sending device hello");

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

    send_debug("Host ack received; Connection complete");

    current_handshake_stage = HandshakeStage::Completed;

    return true;
  }

  return true;
}
