package dev.wycey.mido.fraiselait.orchestrators

import dev.wycey.mido.fraiselait.DevicePortWatcher
import dev.wycey.mido.fraiselait.SerialDevice
import dev.wycey.mido.fraiselait.SerialDevicePhase
import dev.wycey.mido.fraiselait.SerialPortSelection
import dev.wycey.mido.fraiselait.commands.Command
import dev.wycey.mido.fraiselait.models.PinInformation
import dev.wycey.mido.fraiselait.models.TransferMode
import processing.core.PApplet

open class MultiDeviceOrchestrator
  @JvmOverloads
  constructor(
    protected val applet: PApplet,
    val serialRate: Int,
    val transferMode: TransferMode = TransferMode.MSGPACK
  ) {
    private val _devices: MutableSet<SerialDevice> = mutableSetOf()
    val devices: Set<SerialDevice> = _devices

    val deviceIds get() = devices.map { it.id!! }.toSet()

    val deviceMap get() = devices.associateBy { it.id!! }

    init {
      DevicePortWatcher.listen {
        updateDevices(it.toList())
      }
    }

    open fun pinInformationFor(deviceId: String): PinInformation = PinInformation()

    protected open fun setupSerialDevice(port: String): SerialDevice {
      val device =
        SerialDevice(
          applet,
          serialRate,
          SerialPortSelection.Manual(port),
          transferMode,
          pinInformationFor(port)
        )

      device.addPhaseChangeListener {
        when (it) {
          SerialDevicePhase.RUNNING -> {
            _devices.add(device)
          }

          SerialDevicePhase.DISPOSED -> {
            _devices.remove(device)
          }

          SerialDevicePhase.PAUSED -> {
            _devices.remove(device)
          }

          else -> {}
        }
      }

      return device
    }

    protected open fun updateDevices(ports: List<String>) {
      val devicesNeedUpdate = devices.filter { it.port !in ports }

      devicesNeedUpdate.forEach {
        it.dispose()
      }

      val newDevices = ports.filter { port -> devices.none { it.port == port } }

      newDevices.forEach {
        setupSerialDevice(it)
      }
    }

    fun sendSingle(
      deviceId: String,
      command: Command
    ) {
      val device = devices.find { it.port == deviceId } ?: throw IllegalArgumentException("Device not found")

      device.send(command)
    }

    fun sendSingleDirect(
      deviceId: String,
      command: Command
    ) {
      val device = devices.find { it.port == deviceId } ?: throw IllegalArgumentException("Device not found")

      device.sendDirect(command)
    }

    fun sendAll(command: Command) {
      devices.forEach { it.send(command) }
    }
  }
