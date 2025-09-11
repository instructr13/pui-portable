package dev.wycey.mido.fraiselait.cobs

internal object COBS {
  fun encode(input: ByteArray): ByteArray {
    val ret = ByteArray(input.size + input.size / 254 + 2)

    var readIndex = 0
    var writeIndex = 1
    var codeIndex = 0
    var code = 1

    while (readIndex < input.size) {
      if (input[readIndex] == 0.toByte()) {
        ret[codeIndex] = code.toByte()
        codeIndex = writeIndex++

        code = 1

        readIndex++

        continue
      }

      ret[writeIndex++] = input[readIndex++]
      code++

      if (code == 0xFF) {
        ret[codeIndex] = code.toByte()
        codeIndex = writeIndex++

        code = 1
      }
    }

    ret[codeIndex] = code.toByte()
    ret[writeIndex++] = 0

    return ret.copyOf(writeIndex)
  }

  fun decode(input: ByteArray): ByteArray? {
    if (input.isEmpty()) {
      return ByteArray(0)
    }

    val ret = ByteArray(input.size)

    var readIndex = 0
    var writeIndex = 0

    while (readIndex < input.size) {
      val code = input[readIndex].toInt() and 0xFF

      if (code == 0 || readIndex + code > input.size && readIndex + code - 1 != input.size) {
        return null
      }

      readIndex++

      for (i in 1 until code) {
        ret[writeIndex++] = input[readIndex++]
      }

      if (code < 0xFF && readIndex < input.size) {
        ret[writeIndex++] = 0
      }
    }

    return ret.copyOf(writeIndex)
  }
}
