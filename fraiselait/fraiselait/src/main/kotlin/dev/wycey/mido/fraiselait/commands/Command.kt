package dev.wycey.mido.fraiselait.commands

import dev.wycey.mido.fraiselait.constants.COMMAND_DATA_SET

public data class Command(
  private var innerObject: CommandBuilder
) {
  public companion object {
    @JvmStatic
    public val RESET: Command =
      CommandBuilder()
        .changeColor(0, 0, 0)
        .changeLedBuiltin(false)
        .noTone()
        .build()
  }

  internal val flags get() = innerObject.flags
  internal val pinChanges get() = innerObject.pins

  public fun toDataBytes(): ByteArray {
    var data = byteArrayOf(COMMAND_DATA_SET.toByte(), flags.toByte())

    innerObject.pins?.let {
      data += it.toDataBytes()
    }

    innerObject.changeLedBuiltin?.let {
      data += byteArrayOf(if (it) 1 else 0)
    }

    innerObject.tone?.let {
      data += it.toDataBytes()
    }

    innerObject.changeColor?.let {
      data += it.toDataBytes()
    }

    return data
  }

  public fun merge(other: Command): Command = Command(innerObject.merge(other.innerObject))
}
