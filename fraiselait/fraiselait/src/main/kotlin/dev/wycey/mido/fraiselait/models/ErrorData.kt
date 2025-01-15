package dev.wycey.mido.fraiselait.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper

public data class ErrorData
  @JsonCreator
  constructor(
    val code: Short,
    @JvmField
    val message: String
  ) {
    public fun toDataBytes(mapper: ObjectMapper): ByteArray =
      byteArrayOf('E'.code.toByte()) + mapper.writeValueAsBytes(this)
  }

public class SerialException(
  bytes: ByteArray,
  mapper: ObjectMapper,
  data: ErrorData = mapper.readValue(bytes, ErrorData::class.java)
) : RuntimeException(data.message) {
  @JvmField
  public val code: Int = data.code.toInt()
}
