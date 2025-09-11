package dev.wycey.mido.fraiselait.builtins.capability

import dev.wycey.mido.fraiselait.builtins.models.Deserializable
import dev.wycey.mido.fraiselait.builtins.models.Serializable

public interface BaseCapability :
  Serializable,
  Deserializable {
  public val id: Short
  public val minSize: Int
}
