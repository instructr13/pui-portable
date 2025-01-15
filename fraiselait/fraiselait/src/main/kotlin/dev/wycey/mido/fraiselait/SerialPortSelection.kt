package dev.wycey.mido.fraiselait

public sealed class SerialPortSelection {
  public data object Automatic : SerialPortSelection()

  public data class Manual(val port: String? = null) : SerialPortSelection()
}
