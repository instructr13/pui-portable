package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
public data class PinInformation
  @JsonCreator
  constructor(
    val speaker: UByte? = null,
    @JsonProperty("tact_switch")
    val tactSwitch: UByte? = null,
    @JsonProperty("led_green")
    val ledGreen: UByte? = null,
    @JsonProperty("led_blue")
    val ledBlue: UByte? = null,
    @JsonProperty("led_red")
    val ledRed: UByte? = null,
    @JsonProperty("light_sensor")
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
  }

public data class NonNullPinInformation(
  val speaker: UByte,
  @JsonProperty("tact_switch")
  val tactSwitch: UByte,
  @JsonProperty("led_green")
  val ledGreen: UByte,
  @JsonProperty("led_blue")
  val ledBlue: UByte,
  @JsonProperty("led_red")
  val ledRed: UByte,
  @JsonProperty("light_sensor")
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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JVMPinInformation
  @JsonCreator
  constructor(
    @JvmField
    public val speaker: Short? = null,
    @JsonProperty("tact_switch")
    @JvmField
    public val tactSwitch: Short? = null,
    @JsonProperty("led_green")
    @JvmField
    public val ledGreen: Short? = null,
    @JsonProperty("led_blue")
    @JvmField
    public val ledBlue: Short? = null,
    @JsonProperty("led_red")
    @JvmField
    public val ledRed: Short? = null,
    @JsonProperty("light_sensor")
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
  @JsonProperty("tact_switch")
  @JvmField
  val tactSwitch: Short,
  @JsonProperty("led_green")
  @JvmField
  val ledGreen: Short,
  @JsonProperty("led_blue")
  @JvmField
  val ledBlue: Short,
  @JsonProperty("led_red")
  @JvmField
  val ledRed: Short,
  @JsonProperty("light_sensor")
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
