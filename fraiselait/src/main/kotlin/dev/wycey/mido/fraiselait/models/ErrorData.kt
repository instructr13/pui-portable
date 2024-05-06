package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper

data class ErrorData
  @JsonCreator
  constructor(val code: UByte, val message: String) {
    fun toDataBytes(mapper: ObjectMapper) = byteArrayOf('E'.code.toByte()) + mapper.writeValueAsBytes(this)
  }
