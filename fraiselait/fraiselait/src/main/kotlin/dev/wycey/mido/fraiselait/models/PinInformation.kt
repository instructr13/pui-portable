package dev.wycey.mido.fraiselait.models

public data class PinInformation(
  val speaker: UByte? = null,
  val tactSwitch: UByte? = null,
  val ledGreen: UByte? = null,
  val ledBlue: UByte? = null,
  val ledRed: UByte? = null,
  val lightSensor: UByte? = null
) {
  public companion object {
    public fun fromJVMPinInformation(jvmPinInformation: JVMPinInformation): PinInformation =
      PinInformation(
        jvmPinInformation.speaker?.toUByte(),
        jvmPinInformation.tactSwitch?.toUByte(),
        jvmPinInformation.ledGreen?.toUByte(),
        jvmPinInformation.ledBlue?.toUByte(),
        jvmPinInformation.ledRed?.toUByte(),
        jvmPinInformation.lightSensor?.toUByte()
      )
  }

  public fun toJVMPinInformation(): JVMPinInformation = JVMPinInformation.fromPinInformation(this)

  internal fun toDataBytes(): ByteArray =
    byteArrayOf(
      speaker?.toByte() ?: 0,
      tactSwitch?.toByte() ?: 0,
      ledGreen?.toByte() ?: 0,
      ledBlue?.toByte() ?: 0,
      ledRed?.toByte() ?: 0,
      lightSensor?.toByte() ?: 0
    )
}

public data class NonNullPinInformation(
  val speaker: UByte,
  val tactSwitch: UByte,
  val ledGreen: UByte,
  val ledBlue: UByte,
  val ledRed: UByte,
  val lightSensor: UByte
) {
  public fun toPinInformation(): PinInformation =
    PinInformation(
      speaker,
      tactSwitch,
      ledGreen,
      ledBlue,
      ledRed,
      lightSensor
    )

  public fun toJVMNonNullPinInformation(): JVMNonNullPinInformation =
    JVMNonNullPinInformation(
      speaker.toShort(),
      tactSwitch.toShort(),
      ledGreen.toShort(),
      ledBlue.toShort(),
      ledRed.toShort(),
      lightSensor.toShort()
    )

  public fun merge(other: PinInformation): NonNullPinInformation =
    NonNullPinInformation(
      other.speaker ?: speaker,
      other.tactSwitch ?: tactSwitch,
      other.ledGreen ?: ledGreen,
      other.ledBlue ?: ledBlue,
      other.ledRed ?: ledRed,
      other.lightSensor ?: lightSensor
    )
}

public class JVMPinInformation(
  @JvmField
  public val speaker: Short? = null,
  @JvmField
  public val tactSwitch: Short? = null,
  @JvmField
  public val ledGreen: Short? = null,
  @JvmField
  public val ledBlue: Short? = null,
  @JvmField
  public val ledRed: Short? = null,
  @JvmField
  public val lightSensor: Short? = null
) {
  public companion object {
    internal fun fromPinInformation(pinInformation: PinInformation) =
      JVMPinInformation(
        pinInformation.speaker?.toShort(),
        pinInformation.tactSwitch?.toShort(),
        pinInformation.ledGreen?.toShort(),
        pinInformation.ledBlue?.toShort(),
        pinInformation.ledRed?.toShort(),
        pinInformation.lightSensor?.toShort()
      )
  }

  public data class Builder
    @JvmOverloads
    constructor(
      var speaker: Short? = null,
      var tactSwitch: Short? = null,
      var ledGreen: Short? = null,
      var ledBlue: Short? = null,
      var ledRed: Short? = null,
      var lightSensor: Short? = null
    ) {
      public fun build(): JVMPinInformation =
        JVMPinInformation(
          speaker,
          tactSwitch,
          ledGreen,
          ledBlue,
          ledRed,
          lightSensor
        )

      public fun speaker(speaker: Short): Builder = apply { this.speaker = speaker }

      public fun tactSwitch(tactSwitch: Short): Builder = apply { this.tactSwitch = tactSwitch }

      public fun ledGreen(ledGreen: Short): Builder = apply { this.ledGreen = ledGreen }

      public fun ledBlue(ledBlue: Short): Builder = apply { this.ledBlue = ledBlue }

      public fun ledRed(ledRed: Short): Builder = apply { this.ledRed = ledRed }

      public fun lightSensor(lightSensor: Short): Builder = apply { this.lightSensor = lightSensor }
    }

  public fun toPinInformation(): PinInformation = PinInformation.fromJVMPinInformation(this)
}

public data class JVMNonNullPinInformation(
  @JvmField
  val speaker: Short,
  @JvmField
  val tactSwitch: Short,
  @JvmField
  val ledGreen: Short,
  @JvmField
  val ledBlue: Short,
  @JvmField
  val ledRed: Short,
  @JvmField
  val lightSensor: Short
) {
  public fun toNonNullPinInformation(): NonNullPinInformation =
    NonNullPinInformation(
      speaker.toUByte(),
      tactSwitch.toUByte(),
      ledGreen.toUByte(),
      ledBlue.toUByte(),
      ledRed.toUByte(),
      lightSensor.toUByte()
    )

  public fun toJVMPinInformation(): JVMPinInformation =
    JVMPinInformation(
      speaker,
      tactSwitch,
      ledGreen,
      ledBlue,
      ledRed,
      lightSensor
    )
}
