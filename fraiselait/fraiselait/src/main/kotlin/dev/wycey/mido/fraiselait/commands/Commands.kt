package dev.wycey.mido.fraiselait.commands

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

    internal fun toDataBytes(): ByteArray = byteArrayOf(r.toByte(), g.toByte(), b.toByte())
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

      internal fun toDataBytes(): ByteArray {
        val frequency = frequency.toULong()
        val duration = duration ?: 0u

        return byteArrayOf(
          (frequency shr 8).toByte(),
          frequency.toByte(),
          (duration shr 24).toByte(),
          (duration shr 16).toByte(),
          (duration shr 8).toByte(),
          duration.toByte()
        )
      }
    }
}
