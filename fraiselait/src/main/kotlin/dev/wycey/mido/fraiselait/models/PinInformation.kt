package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PinInformation
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
    companion object {
      fun fromJVMPinInformation(jvmPinInformation: JVMPinInformation) =
        PinInformation(
          jvmPinInformation.speaker?.toUByte(),
          jvmPinInformation.tactSwitch?.toUByte(),
          jvmPinInformation.ledGreen?.toUByte(),
          jvmPinInformation.ledBlue?.toUByte(),
          jvmPinInformation.ledRed?.toUByte(),
          jvmPinInformation.lightSensor?.toUByte()
        )
    }

    fun toJVMPinInformation() = JVMPinInformation.fromPinInformation(this)
  }

class JVMPinInformation
  @JsonCreator
  internal constructor(
    val speaker: Short? = null,
    @JsonProperty("tact_switch")
    val tactSwitch: Short? = null,
    @JsonProperty("led_green")
    val ledGreen: Short? = null,
    @JsonProperty("led_blue")
    val ledBlue: Short? = null,
    @JsonProperty("led_red")
    val ledRed: Short? = null,
    @JsonProperty("light_sensor")
    val lightSensor: Short? = null
  ) {
    companion object {
      fun fromPinInformation(pinInformation: PinInformation) =
        JVMPinInformation(
          pinInformation.speaker?.toShort(),
          pinInformation.tactSwitch?.toShort(),
          pinInformation.ledGreen?.toShort(),
          pinInformation.ledBlue?.toShort(),
          pinInformation.ledRed?.toShort(),
          pinInformation.lightSensor?.toShort()
        )
    }

    data class Builder
      @JvmOverloads
      constructor(
        var speaker: Short? = null,
        var tactSwitch: Short? = null,
        var ledGreen: Short? = null,
        var ledBlue: Short? = null,
        var ledRed: Short? = null,
        var lightSensor: Short? = null
      ) {
        fun build() =
          JVMPinInformation(
            speaker,
            tactSwitch,
            ledGreen,
            ledBlue,
            ledRed,
            lightSensor
          )

        fun speaker(speaker: Short) = apply { this.speaker = speaker }

        fun tactSwitch(tactSwitch: Short) = apply { this.tactSwitch = tactSwitch }

        fun ledGreen(ledGreen: Short) = apply { this.ledGreen = ledGreen }

        fun ledBlue(ledBlue: Short) = apply { this.ledBlue = ledBlue }

        fun ledRed(ledRed: Short) = apply { this.ledRed = ledRed }

        fun lightSensor(lightSensor: Short) = apply { this.lightSensor = lightSensor }
      }

    fun toPinInformation() = PinInformation.fromJVMPinInformation(this)
  }
