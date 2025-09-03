package dev.wycey.mido.fraiselait.builtins.commands

public class CommandBuilder {
  internal var changeColor: Commands.ChangeColor? = null
  internal var changeLedBuiltin: Boolean? = null
  internal var tone: Commands.Tone? = null

  public var flags: UByte =
    0u // LSB First, 1st=restoreDefaultPins, 2nd=changePin, 3rd=noTone, 4th=tone, 5th=changeLedBuiltin, 6th=changeColor
    private set

  private fun setFlagFor(position: Int) {
    flags = flags or ((1u shl position).toUByte())
  }

  private fun unsetFlagFor(position: Int) {
    flags = flags and (1u shl position).toUByte().inv()
  }

  private fun hasFlagFor(position: Int) = flags and ((1u shl position).toUByte()) != 0u.toUByte()

  public fun restoreDefaultPins(): CommandBuilder {
    setFlagFor(0)

    if (hasFlagFor(1)) {
      unsetFlagFor(1)
    }

    return this
  }

  public fun unsetRestoreDefaultPins(): CommandBuilder {
    unsetFlagFor(0)

    return this
  }

  public fun noTone(): CommandBuilder {
    setFlagFor(2)

    if (hasFlagFor(3)) {
      tone = null

      unsetFlagFor(3)
    }

    return this
  }

  public fun unsetNoTone(): CommandBuilder {
    unsetFlagFor(2)

    return this
  }

  @JvmOverloads
  public fun tone(
    frequency: Int,
    duration: Long? = null
  ): CommandBuilder {
    tone = Commands.Tone(frequency, duration)
    setFlagFor(3)

    unsetFlagFor(2)

    return this
  }

  public fun unsetTone(): CommandBuilder {
    tone = null
    unsetFlagFor(3)

    return this
  }

  public fun changeLedBuiltin(state: Boolean): CommandBuilder {
    changeLedBuiltin = state
    setFlagFor(4)

    return this
  }

  public fun unsetChangeLedBuiltin(): CommandBuilder {
    changeLedBuiltin = null
    unsetFlagFor(4)

    return this
  }

  public fun changeColor(
    r: UByte,
    g: UByte,
    b: UByte
  ): CommandBuilder {
    changeColor = Commands.ChangeColor(r, g, b)
    setFlagFor(5)

    return this
  }

  public fun changeColor(
    r: Int,
    g: Int,
    b: Int
  ): CommandBuilder {
    changeColor = Commands.ChangeColor(r, g, b)
    setFlagFor(5)

    return this
  }

  public fun unsetChangeColor(): CommandBuilder {
    changeColor = null
    unsetFlagFor(5)

    return this
  }

  internal fun merge(other: CommandBuilder): CommandBuilder {
    val newChangeColor = other.changeColor ?: changeColor
    val newChangeLedBuiltin = other.changeLedBuiltin ?: changeLedBuiltin
    val newTone = other.tone ?: tone

    val newFlags = flags or other.flags

    return CommandBuilder().apply {
      changeColor = newChangeColor
      changeLedBuiltin = newChangeLedBuiltin
      tone = newTone
      flags = newFlags
    }
  }

  public fun build(): Command = Command(this)
}
