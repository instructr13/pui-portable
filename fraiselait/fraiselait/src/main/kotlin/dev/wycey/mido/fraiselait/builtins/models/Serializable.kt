package dev.wycey.mido.fraiselait.builtins.models

import dev.wycey.mido.fraiselait.util.VariableByteBuffer
import java.nio.ByteOrder

public interface Serializable {
  public fun serialize(buffer: VariableByteBuffer)

  public fun toByteArray(): ByteArray {
    val buffer = VariableByteBuffer(ByteOrder.LITTLE_ENDIAN)

    serialize(buffer)

    return buffer.array.copyOf(buffer.size)
  }
}
