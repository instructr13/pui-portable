package dev.wycey.mido.fraiselait.builtins

public enum class WaveformType(
  internal val code: UShort
) {
  SQUARE(0x0001u),
  SQUARE_25(0x0002u),
  SQUARE_12(0x0003u),
  TRIANGLE(0x0004u),
  SAW(0x0005u),
  SINE(0x0006u),
  NOISE(0x0007u)

  ;

  internal companion object {
    fun fromCode(value: UShort): WaveformType? = WaveformType.entries.find { it.code == value }
  }
}
