package dev.wycey.mido.fraiselait.commands

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class Command(private var innerObject: CommandBuilder) {
  companion object {
    private val mapper = jacksonObjectMapper()

    @JvmStatic
    val RESET = CommandBuilder().changeColor(0, 0, 0).changeLedBuiltin(false).noTone().build()
  }

  val flags get() = innerObject.flags
  val data: ByteArray get() = mapper.writeValueAsBytes(innerObject)

  fun toDataBytes(): ByteArray {
    val flagsBytes = flags.toString().toByteArray()

    return flagsBytes + data
  }

  fun merge(other: Command) = Command(innerObject.merge(other.innerObject))

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Command

    if (flags != other.flags) return false
    if (!data.contentEquals(other.data)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = flags.hashCode()
    result = 31 * result + data.contentHashCode()
    return result
  }
}
