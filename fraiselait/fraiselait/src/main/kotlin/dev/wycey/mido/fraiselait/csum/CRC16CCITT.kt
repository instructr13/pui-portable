package dev.wycey.mido.fraiselait.csum

internal object CRC16CCITT {
  private const val POLYNOMIAL = 0x1021

  private val table =
    IntArray(256) { i ->
      var crc = i shl 8

      repeat(8) {
        crc =
          if (crc and 0x8000 != 0) {
            (crc shl 1) xor POLYNOMIAL
          } else {
            crc shl 1
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

    var crc = 0x0000

    for (i in index until index + length) {
      val byte = data[i].toInt() and 0xFF
      val tableIndex = ((crc ushr 8) xor byte) and 0xFF

      crc = ((crc shl 8) xor table[tableIndex]) and 0xFFFF
    }

    return crc.toUShort()
  }

  fun verify(
    data: ByteArray,
    expected: UShort,
    index: Int = 0,
    length: Int = data.size - index
  ) = compute(data, index, length) == expected
}
