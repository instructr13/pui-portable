package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper

data class ErrorData
  @JsonCreator
  constructor(
    val code: Short,
    @JvmField
    val message: String
  ) {
    fun toDataBytes(mapper: ObjectMapper) = byteArrayOf('E'.code.toByte()) + mapper.writeValueAsBytes(this)
  }

class SerialException(
  bytes: ByteArray,
  mapper: ObjectMapper,
  data: ErrorData = mapper.readValue(bytes, ErrorData::class.java)
) : RuntimeException(data.message) {
  @JvmField
  val code = data.code.toInt()
}
