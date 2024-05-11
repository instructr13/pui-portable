package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

internal data class DeviceInformation
  @JsonCreator
  constructor(
    val version: Int,
    @JsonProperty("device_id")
    @JvmField
    val deviceId: String,
    @JvmField
    val pins: JVMNonNullPinInformation
  )
