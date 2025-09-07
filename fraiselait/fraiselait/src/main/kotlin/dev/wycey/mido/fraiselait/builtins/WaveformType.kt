package dev.wycey.mido.fraiselait.builtins

public enum class WaveformType(
  internal val code: UShort
) {
  Square(0x0001u),
  Square25(0x0002u),
  Square12(0x0003u),
  Triangle(0x0004u),
  Saw(0x0005u),
  Sine(0x0006u),
  Noise(0x0007u)

  ;

  internal companion object {
    fun fromCode(value: UShort): WaveformType? = WaveformType.entries.find { it.code == value }
  }
}
