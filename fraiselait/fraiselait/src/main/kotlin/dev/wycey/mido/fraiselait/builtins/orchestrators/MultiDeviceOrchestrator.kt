package dev.wycey.mido.fraiselait.builtins.orchestrators

import dev.wycey.mido.fraiselait.builtins.ConnectionStatus
import dev.wycey.mido.fraiselait.builtins.DevicePortWatcher
import dev.wycey.mido.fraiselait.builtins.FraiselaitDevice
import dev.wycey.mido.fraiselait.builtins.SerialPortSelection
import dev.wycey.mido.fraiselait.builtins.commands.Command

public open class MultiDeviceOrchestrator(
  public val serialRate: Int
) {
  private val _devices: MutableMap<String, FraiselaitDevice> = mutableMapOf()
  public val devices: Set<FraiselaitDevice>
    get() = _devices.values.toSet()

  private val _namedDevices: MutableMap<String, FraiselaitDevice> = mutableMapOf()
  public val namedDevices: Map<String, FraiselaitDevice> = _namedDevices

  protected open fun setupSerialDevice(port: String): FraiselaitDevice =
    FraiselaitDevice(
      serialRate,
      SerialPortSelection.Manual(port),
      listOf()
    ).apply {
      onStatusChange { status ->
        when (status) {
          ConnectionStatus.CONNECTING -> {
            println("Connecting to device: $port")
          }

          ConnectionStatus.CONNECTED -> {
            if (_devices.put(id!!, this) != null) {
              throw IllegalStateException("New device $this is already in the devices set")
            }

            retrieveStateForever = true

            println("New device: $id")
          }

          ConnectionStatus.NOT_CONNECTED -> {
            if (_devices.remove(id!!) == null) {
              throw IllegalStateException("Disconnected device $this was not in the devices set")
            }

            println("Disconnected device: $id")

            if (_namedDevices.entries.removeIf { it.value.id == id }) {
              println("Removed named device: $id")
            }
          }

          ConnectionStatus.DISPOSED -> {
            if (id == null) {
              println("Disposed device with no ID: $port")

              return@onStatusChange
            }

            if (_devices.remove(id) == null) {
              throw IllegalStateException("Disposed device $this was not in the devices set")
            }

            println("Removed device: $id")

            if (_namedDevices.entries.removeIf { it.value.id == id }) {
              println("Removed device name: $id")
            }
          }
        }
      }
    }

  protected open fun disposeSerialDevice(device: FraiselaitDevice) {
    device.dispose()
  }

  protected open fun updateDevices(ports: List<String>) {
    fun isMacOs(): Boolean = System.getProperty("os.name").lowercase().contains("mac")

    val devicesToBeRemoved = _devices.values.filter { it.port !in ports }

    devicesToBeRemoved.forEach {
      disposeSerialDevice(it)
    }

    val newDevices =
      ports.filter { port -> _devices.values.none { it.port == port } }.filter {
        // Only include /dev/cu.usbmodem* ports on macOS
        if (isMacOs()) {
          it.startsWith("/dev/cu.usbmodem")
        } else {
          true
        }
      }

    newDevices.forEach {
      setupSerialDevice(it)
    }
  }

  public fun sendAll(command: Command) {
    _devices.values.forEach { it.sendCommand(command) }
  }

  public fun getDevice(deviceId: String): FraiselaitDevice? = _devices[deviceId]

  public fun nameDevice(
    deviceId: String,
    name: String
  ) {
    val device = getDevice(deviceId) ?: return

    _namedDevices[name] = device
  }

  public fun getNamedDevice(name: String): FraiselaitDevice? = _namedDevices[name]

  public fun unnameDevice(name: String) {
    _namedDevices.remove(name)
  }

  public fun start() {
    DevicePortWatcher.start()

    DevicePortWatcher.listen(::serialPortListener)
  }

  public fun stop() {
    DevicePortWatcher.unlisten(::serialPortListener)
  }

  private fun serialPortListener(strings: Array<String>) {
    updateDevices(strings.toList())
  }
}
