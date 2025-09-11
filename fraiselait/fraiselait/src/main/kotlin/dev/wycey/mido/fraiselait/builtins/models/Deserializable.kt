package dev.wycey.mido.fraiselait.builtins.models

import java.nio.ByteBuffer
import java.nio.ByteOrder

public interface Deserializable {
  public fun deserialize(data: ByteBuffer): Boolean

  public fun fromByteArray(data: ByteArray): Boolean {
    val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    return deserialize(buffer)
  }
}
