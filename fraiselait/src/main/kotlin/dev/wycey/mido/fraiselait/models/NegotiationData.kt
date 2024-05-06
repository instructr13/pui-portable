package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import dev.wycey.mido.fraiselait.constants.PROTOCOL_VERSION

data class NegotiationData
  @JsonCreator
  constructor(val pins: PinInformation) {
    val version = PROTOCOL_VERSION
  }
