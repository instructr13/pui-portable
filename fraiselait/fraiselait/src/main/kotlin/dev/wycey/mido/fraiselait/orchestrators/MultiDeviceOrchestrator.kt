package dev.wycey.mido.fraiselait.orchestrators

import dev.wycey.mido.fraiselait.DevicePortWatcher
import dev.wycey.mido.fraiselait.SerialDevice
import dev.wycey.mido.fraiselait.SerialDevicePhase
import dev.wycey.mido.fraiselait.SerialPortSelection
import dev.wycey.mido.fraiselait.commands.Command
import dev.wycey.mido.fraiselait.models.PinInformation
import dev.wycey.mido.fraiselait.models.TransferMode
import processing.core.PApplet

public open class MultiDeviceOrchestrator
  @JvmOverloads
  constructor(
    protected val applet: PApplet,
    public val serialRate: Int,
    public val transferMode: TransferMode = TransferMode.MSGPACK
  ) {
    private val _devices: MutableSet<SerialDevice> = mutableSetOf()
    public val devices: Set<SerialDevice> = _devices

    init {
      DevicePortWatcher.listen {
        updateDevices(it.toList())
      }
    }

    public open fun pinInformationFor(deviceId: String): PinInformation = PinInformation()

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

    public fun sendSingle(
      deviceId: String,
      command: Command,
      buffered: Boolean = true
    ) {
      val device = devices.find { it.port == deviceId } ?: throw IllegalArgumentException("Device not found")

      device.send(command, buffered)
    }

    public fun sendAll(command: Command) {
      devices.forEach { it.send(command) }
    }
  }
