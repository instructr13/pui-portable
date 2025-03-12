package dev.wycey.mido.fraiselait.models

import java.nio.ByteBuffer

internal data class DeviceInformation(
  val version: Int,
  val deviceId: String,
  val pins: JVMNonNullPinInformation
) {
  internal companion object {
    @OptIn(ExperimentalStdlibApi::class)
    internal fun fromData(data: ByteArray): DeviceInformation {
      val version = ByteBuffer.wrap(data.copyOfRange(0, 2)).short.toInt()
      val deviceId = data.copyOfRange(2, 6).toHexString()
      val pins =
        JVMNonNullPinInformation(
          speaker = data[6].toShort(),
          tactSwitch = data[7].toShort(),
          ledGreen = data[8].toShort(),
          ledBlue = data[9].toShort(),
          ledRed = data[10].toShort(),
          lightSensor = data[11].toShort()
        )

      return DeviceInformation(version, deviceId, pins)
    }
  }
}
