package dev.wycey.mido.fraiselait.commands

import com.fasterxml.jackson.databind.ObjectMapper

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

  public fun toDataBytes(
    mapper: ObjectMapper,
    deviceId: String
  ): ByteArray {
    val flagsBytes = flags.toString().toByteArray()
    val deviceIdBytes = deviceId.toByteArray()
    val data = mapper.writeValueAsBytes(innerObject)

    return flagsBytes + deviceIdBytes + data
  }

  public fun merge(other: Command): Command = Command(innerObject.merge(other.innerObject))
}
