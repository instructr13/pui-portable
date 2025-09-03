package dev.wycey.mido.fraiselait.packet

internal enum class ReservedErrorCode(
  val code: UShort
) {
  UNKNOWN_PACKET_TYPE(0x0001u),
  MALFORMED_PACKET(0x0002u),
  UNSUPPORTED_PROTOCOL_VERSION(0x0003u),
  MISSING_CAPABILITIES(0x0004u),
  HANDSHAKE_NOT_COMPLETED(0x0005u),
  INTERNAL_ERROR(0x00FFu)

  ;

  companion object {
    fun fromCode(value: UShort): ReservedErrorCode? = entries.find { it.code == value }
  }
}
