package dev.wycey.mido.fraiselait.packet

import dev.wycey.mido.fraiselait.csum.CRC8
import dev.wycey.mido.fraiselait.util.VariableByteBuffer
import java.nio.ByteBuffer

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

internal data class Packet(
  val type: PacketType,
  val payload: ByteArray = byteArrayOf()
) {
  companion object {
    fun parse(data: VariableByteBuffer): Packet? {
      if (data.size < 3) return null

      val crc = data.get()

      if (!CRC8.verify(data.array, crc, 1, data.size - 1)) return null

      val rawType = data.getShort().toUShort()
      val type =
        PacketType.fromCode(rawType) ?: run {
          println("Unknown packet type: $rawType")

          return null
        }

      val payload = ByteArray(data.size - (1 + 2))

      data.get(payload)

      return Packet(type, payload)
    }
  }

  fun encode(): ByteArray {
    // payload without CRC8
    val rawPayload = ByteBuffer.allocate(2 + payload.size)

    rawPayload.putShort(type.code.toShort())
    rawPayload.put(payload)

    val rawPayloadArray = rawPayload.array()

    val crc = CRC8.compute(rawPayloadArray, 0, rawPayloadArray.size)
    val packetData = ByteBuffer.allocate(1 + rawPayloadArray.size)

    packetData.put(crc)
    packetData.put(rawPayloadArray)

    return packetData.array()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Packet) return false

    if (type != other.type) return false
    if (!payload.contentEquals(other.payload)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = type.hashCode()
    result = 31 * result + payload.contentHashCode()
    return result
  }
}
