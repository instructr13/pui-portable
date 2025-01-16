package dev.wycey.mido.fraiselait.commands

internal sealed class Commands {
  internal class ChangeColor(
    rawR: Int,
    rawG: Int,
    rawB: Int
  ) : Commands() {
    val r: UByte = rawR.toUByte()
    val g: UByte = rawG.toUByte()
    val b: UByte = rawB.toUByte()
  }

  internal class Tone
    @JvmOverloads
    constructor(
      rawFrequency: Int,
      rawDuration: Long? = null
    ) : Commands() {
      val frequency: UShort = rawFrequency.toUShort()
      val duration: UInt? = rawDuration?.toUInt()
    }
}
