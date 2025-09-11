package dev.wycey.mido.fraiselait.builtins

import dev.wycey.mido.fraiselait.builtins.models.Deserializable
import java.nio.ByteBuffer

public data class FraiselaitDeviceState(
  var buttonPressing: Boolean = false,
  var lightStrength: Int = 0,
  var temperature: Float = 0f
) : Deserializable {
  override fun deserialize(data: ByteBuffer): Boolean {
    if (data.remaining() < 1 + 4 + 4) return false

    buttonPressing = data.get().toInt() != 0
    lightStrength = data.int
    temperature = data.float

    return true
  }
}
