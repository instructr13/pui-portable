package dev.wycey.mido.fraiselait.builtins

public enum class WaveformType(
  public val tokenName: String,
  public val displayName: String,
  internal val code: UShort
) {
  SQUARE("square", "Square (50%)", 0x0001u),
  SQUARE_25("square25", "Square (25%)", 0x0002u),
  SQUARE_12("square12", "Square (12%)", 0x0003u),
  TRIANGLE("triangle", "Triangle", 0x0004u),
  SAW("saw", "Saw", 0x0005u),
  SINE("sine", "Sine", 0x0006u),
  NOISE("noise", "Noise", 0x0007u)

  ;

  public companion object {
    @JvmStatic
    public fun fromTokenName(value: String): WaveformType? =
      WaveformType.entries.find { it.tokenName.equals(value, ignoreCase = true) }

    internal fun fromCode(value: UShort): WaveformType? = WaveformType.entries.find { it.code == value }
  }
}
