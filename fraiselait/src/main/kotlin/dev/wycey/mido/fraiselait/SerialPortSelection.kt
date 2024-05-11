package dev.wycey.mido.fraiselait

sealed class SerialPortSelection private constructor() {
  data object Automatic : SerialPortSelection()

  data class Manual(val port: String? = null) : SerialPortSelection()
}
