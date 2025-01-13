package dev.wycey.mido.fraiselait.models

enum class TransferMode {
  JSON,
  MSGPACK

  ;

  fun toByte() =
    when (this) {
      JSON -> 'J'.code.toByte()
      MSGPACK -> 'M'.code.toByte()
    }
}
