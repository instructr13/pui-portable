package dev.wycey.mido.fraiselait.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import dev.wycey.mido.fraiselait.models.JVMPinInformation
import dev.wycey.mido.fraiselait.models.PinInformation

@JsonInclude(JsonInclude.Include.NON_NULL)
class CommandBuilder {
  @JsonProperty("color")
  private var changeColor: Commands.ChangeColor? = null

  @JsonProperty("led_builtin")
  private var changeLedBuiltin: Boolean? = null

  @JsonProperty
  private var tone: Commands.Tone? = null

  @JsonProperty
  private var pins: PinInformation? = null

  @JsonIgnore
  var flags =
    0u // LE, 1st=changeColor, 2nd=changeLedBuiltin, 3rd=tone, 4th=noTone, 5th=changePin, 6th=restoreDefaultPins
    private set

  private fun setFlagFor(position: Int) {
    flags = flags or (1u shl position)
  }

  private fun unsetFlagFor(position: Int) {
    flags = flags and (1u shl position).inv()
  }

  private fun hasFlagFor(position: Int) = flags and (1u shl position) != 0u

  fun changeColor(
    r: Int,
    g: Int,
    b: Int
  ): CommandBuilder {
    changeColor = Commands.ChangeColor(r, g, b)
    setFlagFor(0)

    return this
  }

  fun unsetChangeColor(): CommandBuilder {
    changeColor = null
    unsetFlagFor(0)

    return this
  }

  fun changeLedBuiltin(state: Boolean): CommandBuilder {
    changeLedBuiltin = state
    setFlagFor(1)

    return this
  }

  fun unsetChangeLedBuiltin(): CommandBuilder {
    changeLedBuiltin = null
    unsetFlagFor(1)

    return this
  }

  @JvmOverloads
  fun tone(
    frequency: Int,
    duration: Long? = null
  ): CommandBuilder {
    tone = Commands.Tone(frequency, duration)
    setFlagFor(2)

    unsetFlagFor(3)

    return this
  }

  fun unsetTone(): CommandBuilder {
    tone = null
    unsetFlagFor(2)

    return this
  }

  fun noTone(): CommandBuilder {
    setFlagFor(3)

    if (hasFlagFor(2)) {
      tone = null

      unsetFlagFor(2)
    }

    return this
  }

  fun unsetNoTone(): CommandBuilder {
    unsetFlagFor(3)

    return this
  }

  fun changePin(pins: PinInformation): CommandBuilder {
    this.pins = pins
    setFlagFor(4)

    return this
  }

  fun changePin(pins: JVMPinInformation): CommandBuilder {
    this.pins = PinInformation.fromJVMPinInformation(pins)
    setFlagFor(4)

    return this
  }

  fun unsetChangePin(): CommandBuilder {
    pins = null
    unsetFlagFor(4)

    return this
  }

  fun restoreDefaultPins(): CommandBuilder {
    setFlagFor(5)

    if (hasFlagFor(4)) {
      pins = null

      unsetFlagFor(4)
    }

    return this
  }

  fun unsetRestoreDefaultPins(): CommandBuilder {
    unsetFlagFor(5)

    return this
  }

  internal fun merge(other: CommandBuilder): CommandBuilder {
    val newChangeColor = other.changeColor ?: changeColor
    val newChangeLedBuiltin = other.changeLedBuiltin ?: changeLedBuiltin
    val newTone = other.tone ?: tone
    val newPins = other.pins ?: pins

    val newFlags = flags or other.flags

    return CommandBuilder().apply {
      changeColor = newChangeColor
      changeLedBuiltin = newChangeLedBuiltin
      tone = newTone
      pins = newPins
      flags = newFlags
    }
  }

  fun build() = Command(this)
}
