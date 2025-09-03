package dev.wycey.mido.fraiselait.models

import java.nio.ByteBuffer

public interface Deserializable {
  public fun deserialize(data: ByteBuffer): Boolean

  public fun fromByteArray(data: ByteArray): Boolean {
    val buffer = ByteBuffer.wrap(data)

    return deserialize(buffer)
  }
}
