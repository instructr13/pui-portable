package dev.wycey.mido.fraiselait.commands

import com.fasterxml.jackson.databind.ObjectMapper

data class Command(private var innerObject: CommandBuilder) {
  companion object {
    @JvmStatic
    val RESET = CommandBuilder().changeColor(0, 0, 0).changeLedBuiltin(false).noTone().build()
  }

  val flags get() = innerObject.flags
  val pinChanges get() = innerObject.pins

  fun toDataBytes(
    mapper: ObjectMapper,
    deviceId: String
  ): ByteArray {
    val flagsBytes = flags.toString().toByteArray()
    val deviceIdBytes = deviceId.toByteArray()
    val data = mapper.writeValueAsBytes(innerObject)

    return flagsBytes + deviceIdBytes + data
  }

  fun merge(other: Command) = Command(innerObject.merge(other.innerObject))
}
