package dev.wycey.mido.fraiselait.csum

internal object CRC8 {
  private const val POLYNOMIAL = 0x07

  private val table =
    IntArray(256) { i ->
      var crc = i

      for (j in 0 until 8) {
        crc =
          if (crc and 0x80 != 0) {
            (crc shl 1) xor POLYNOMIAL
          } else {
            crc shl 1
          }
      }
      crc and 0xFF
    }

  fun compute(
    data: ByteArray,
    index: Int = 0,
    length: Int = data.size - index
  ): Byte {
    if (index < 0 || length < 0 || index + length > data.size) {
      throw IndexOutOfBoundsException("Index: $index, Length: $length, Data Size: ${data.size}")
    }

    var crc = 0

    for (i in index until index + length) {
      val byte = data[i].toInt() and 0xFF

      crc = table[crc xor byte]
    }

    return crc.toByte()
  }

  fun verify(
    data: ByteArray,
    expected: Byte,
    index: Int = 0,
    length: Int = data.size - index
  ): Boolean {
    if (index < 0 || length < 0 || index + length > data.size) {
      throw IndexOutOfBoundsException("Index: $index, Length: $length, Data Size: ${data.size}")
    }

    return compute(data, index, length) == expected
  }
}
