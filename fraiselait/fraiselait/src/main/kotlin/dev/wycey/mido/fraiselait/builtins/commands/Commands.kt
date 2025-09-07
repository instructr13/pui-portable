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
      val frequency: Float,
      val volume: Float = 1f,
      val duration: UInt? = null
    ) : Commands() {
      internal constructor(
        rawFrequency: Float,
        rawVolume: Float = 1f,
        rawDuration: Long? = null
      ) : this(
        rawFrequency,
        rawVolume,
        rawDuration?.toUInt()
      )
    }
}
