package dev.wycey.mido.fraiselait.packet

internal enum class PacketType(
  val code: UShort
) {
  HOST_HELLO(0x0001u),
  DEVICE_HELLO(0x0002u),
  HOST_ACK(0x0003u),
  DATA(0x0004u),
  ERROR(0x0005u)

  ;

  companion object {
    fun fromCode(value: UShort): PacketType? = entries.find { it.code == value }
  }
}
