package dev.wycey.mido.fraiselait

public sealed class SerialPortSelection {
  public data object FirstAvailable : SerialPortSelection()

  public data class Manual(
    val port: String? = null
  ) : SerialPortSelection()
}
