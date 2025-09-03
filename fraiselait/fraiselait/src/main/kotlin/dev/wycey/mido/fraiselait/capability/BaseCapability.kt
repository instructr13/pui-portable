package dev.wycey.mido.fraiselait.capability

import dev.wycey.mido.fraiselait.models.Deserializable
import dev.wycey.mido.fraiselait.models.Serializable

public interface BaseCapability :
  Serializable,
  Deserializable {
  public val id: Short
  public val minSize: Int
}
