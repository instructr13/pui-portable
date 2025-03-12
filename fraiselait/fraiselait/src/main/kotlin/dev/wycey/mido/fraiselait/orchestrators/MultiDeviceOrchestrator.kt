package dev.wycey.mido.fraiselait.orchestrators

import dev.wycey.mido.fraiselait.DevicePortWatcher
import dev.wycey.mido.fraiselait.SerialDevice
import dev.wycey.mido.fraiselait.SerialDevicePhase
import dev.wycey.mido.fraiselait.SerialPortSelection
import dev.wycey.mido.fraiselait.commands.Command
import processing.core.PApplet

public open class MultiDeviceOrchestrator(
  protected val applet: PApplet,
  public val serialRate: Int
) {
  private val _devices: MutableMap<String, SerialDevice> = mutableMapOf()
  public val devices: Set<SerialDevice>
    get() = _devices.values.toSet()

  private val _namedDevices: MutableMap<String, SerialDevice> = mutableMapOf()
  public val namedDevices: Map<String, SerialDevice> = _namedDevices

  init {
    DevicePortWatcher.listen {
      updateDevices(it.toList())
    }
  }

  protected open fun setupSerialDevice(port: String): SerialDevice {
    val device =
      SerialDevice(
        applet,
        serialRate,
        SerialPortSelection.Manual(port)
      )

    device.addPhaseChangeListener {
      when (it) {
        SerialDevicePhase.RUNNING -> {
          if (_devices.put(device.id!!, device) != null) {
            throw IllegalStateException("New device $device is already in the devices set")
          }

          println("New device: ${device.id}")
        }

        SerialDevicePhase.DISPOSED -> {
          if (_devices.remove(device.id!!) == null) {
            throw IllegalStateException("Disposed device $device was not in the devices set")
          }

          println("Removed device: ${device.id}")

          if (_namedDevices.entries.removeIf { it.value.id == device.id }) {
            println("Removed device name: ${device.id}")
          }
        }

        SerialDevicePhase.PAUSED -> {
          if (_devices.remove(device.id!!) == null) {
            throw IllegalStateException("Paused device $device was not in the devices set")
          }

          println("Paused device: ${device.id}")

          if (_namedDevices.entries.removeIf { it.value.id == device.id }) {
            println("Removed named device: ${device.id}")
          }
        }

        else -> {}
      }
    }

    return device
  }

  protected open fun disposeSerialDevice(device: SerialDevice) {
    device.dispose()
  }

  protected open fun updateDevices(ports: List<String>) {
    val devicesToBeRemoved = _devices.values.filter { it.port !in ports }

    devicesToBeRemoved.forEach {
      disposeSerialDevice(it)
    }

    val newDevices = ports.filter { port -> _devices.values.none { it.port == port } }

    newDevices.forEach {
      setupSerialDevice(it)
    }
  }

  @JvmOverloads
  public fun sendAll(
    command: Command,
    buffered: Boolean = true
  ) {
    _devices.values.forEach { it.send(command, true) }
  }

  public fun getDevice(deviceId: String): SerialDevice? = _devices[deviceId]

  public fun nameDevice(
    deviceId: String,
    name: String
  ) {
    val device = getDevice(deviceId) ?: return

    _namedDevices[name] = device
  }

  public fun getNamedDevice(name: String): SerialDevice? = _namedDevices[name]

  public fun unnameDevice(name: String) {
    _namedDevices.remove(name)
  }

  public fun start() {
    DevicePortWatcher.start(applet)
  }

  public fun stop() {
    DevicePortWatcher.dispose()
  }
}
