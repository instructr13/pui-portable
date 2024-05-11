package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import dev.wycey.mido.fraiselait.constants.PROTOCOL_VERSION

internal data class NegotiationData
  @JsonCreator
  constructor(
    @JvmField
    val pins: PinInformation
  ) {
    @JsonProperty
    private val version = PROTOCOL_VERSION
  }
