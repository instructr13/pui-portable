package dev.wycey.mido.fraiselait.csum

internal object CRC16CCITT {
  private const val POLYNOMIAL = 0x1021

  private val table =
    IntArray(256) { i ->
      var crc = i

      for (j in 0 until 8) {
        crc =
          if (crc and 0x0001 != 0) {
            (crc ushr 1) xor POLYNOMIAL
          } else {
            crc ushr 1
          }
      }

      crc and 0xFFFF
    }

  fun compute(
    data: ByteArray,
    index: Int = 0,
    length: Int = data.size - index
  ): UShort {
    if (index < 0 || length < 0 || index + length > data.size) {
      throw IndexOutOfBoundsException("Index: $index, Length: $length, Data Size: ${data.size}")
    }

    var crc = 0xFFFF

    for (i in index until index + length) {
      val byte = data[i].toInt() and 0xFF
      val index = (crc xor byte) and 0xFF

      crc = table[index] xor (crc ushr 8)
    }

    return (crc and 0xFFFF).toUShort()
  }

  fun verify(
    data: ByteArray,
    expected: UShort,
    index: Int = 0,
    length: Int = data.size - index
  ) = compute(data, index, length) == expected
}
