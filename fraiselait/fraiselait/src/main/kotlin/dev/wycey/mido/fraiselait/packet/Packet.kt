package dev.wycey.mido.fraiselait.packet

public data class Packet(
  val type: UShort,
  val payload: ByteArray = byteArrayOf()
) {
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

  override fun toString(): String =
    "Packet(type=$type, payload=${payload.joinToString(", ") { "0x" + it.toUByte().toString(16).padStart(2, '0') }})"
}
