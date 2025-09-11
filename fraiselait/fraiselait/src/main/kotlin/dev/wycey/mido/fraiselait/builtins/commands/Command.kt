package dev.wycey.mido.fraiselait.builtins.commands

import dev.wycey.mido.fraiselait.builtins.models.Serializable
import dev.wycey.mido.fraiselait.util.VariableByteBuffer

public data class Command(
  private var innerObject: CommandBuilder
) : Serializable {
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

  public fun merge(other: Command): Command = Command(innerObject.merge(other.innerObject))

  override fun serialize(buffer: VariableByteBuffer) {
    buffer.put(flags.toByte())

    innerObject.waveformType?.let {
      buffer.putShort(it.code.toShort())
    }

    innerObject.changeColor?.let {
      buffer.put(it.r.toByte())
      buffer.put(it.g.toByte())
      buffer.put(it.b.toByte())
    }

    innerObject.changeLedBuiltin?.let {
      buffer.put(if (it) 1.toByte() else 0.toByte())
    }

    innerObject.tone?.let {
      buffer.putFloat(it.frequency)
      buffer.putFloat(it.volume)

      buffer.putInt(it.duration?.toInt() ?: 0)
    }
  }
}
