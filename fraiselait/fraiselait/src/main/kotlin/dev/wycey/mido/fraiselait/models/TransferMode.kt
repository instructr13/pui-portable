package dev.wycey.mido.fraiselait.models

public enum class TransferMode {
  JSON,
  MSGPACK

  ;

  internal fun toByte() =
    when (this) {
      JSON -> 'J'.code.toByte()
      MSGPACK -> 'M'.code.toByte()
    }
}
