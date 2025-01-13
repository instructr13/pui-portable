package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class DeviceState
  @JsonCreator
  constructor(
    @JsonProperty("button_pressing")
    @JvmField
    val buttonPressing: Boolean,
    @JsonProperty("light_strength")
    @JvmField
    val lightStrength: Long,
    @JvmField
    val temperature: Double
  )
