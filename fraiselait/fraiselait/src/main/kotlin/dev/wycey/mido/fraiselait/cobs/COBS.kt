package dev.wycey.mido.fraiselait.cobs

internal object COBS {
  internal enum class DecodeStatus {
    IN_PROGRESS,
    COMPLETE,
    ERROR
  }

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
        readIndex++

        code = 1

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

  fun decode(input: ByteArray): Pair<DecodeStatus, ByteArray> {
    val ret = ByteArray(input.size)

    var readIndex = 0
    var writeIndex = 0

    while (readIndex < input.size) {
      val code = input[readIndex].toInt() and 0xFF

      if (code == 0 || readIndex + code > input.size && readIndex + code - 1 != input.size) {
        return Pair(DecodeStatus.ERROR, ByteArray(0))
      }

      readIndex++

      for (i in 1 until code) {
        if (readIndex >= input.size) {
          return Pair(DecodeStatus.IN_PROGRESS, ret.copyOf(writeIndex))
        }

        ret[writeIndex++] = input[readIndex++]
      }

      if (code != 0xFF && readIndex < input.size) {
        ret[writeIndex++] = 0
      }
    }

    return Pair(DecodeStatus.COMPLETE, ret.copyOf(writeIndex))
  }
}
