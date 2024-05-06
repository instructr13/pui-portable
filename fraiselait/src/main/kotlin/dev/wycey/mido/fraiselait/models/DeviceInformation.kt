package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class DeviceInformation
  @JsonCreator
  constructor(
    val version: UInt,
    @JsonProperty("device_id") val deviceId: String
  )
