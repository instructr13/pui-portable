package dev.wycey.mido.fraiselait.builtins.commands

internal sealed class Commands {
  internal data class ChangeColor(
    val r: UByte,
    val g: UByte,
    val b: UByte
  ) : Commands() {
    internal constructor(
      rawR: Int,
      rawG: Int,
      rawB: Int
    ) : this(
      rawR.toUByte(),
      rawG.toUByte(),
      rawB.toUByte()
    )
  }

  internal data class Tone
    @JvmOverloads
    constructor(
      val frequency: UShort,
      val duration: UInt? = null
    ) : Commands() {
      internal constructor(
        rawFrequency: Int,
        rawDuration: Long? = null
      ) : this(
        rawFrequency.toUShort(),
        rawDuration?.toUInt()
      )
    }
}
