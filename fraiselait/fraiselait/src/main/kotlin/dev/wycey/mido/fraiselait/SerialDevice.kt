package dev.wycey.mido.fraiselait

import dev.wycey.mido.fraiselait.commands.Command
import dev.wycey.mido.fraiselait.constants.*
import dev.wycey.mido.fraiselait.coroutines.sync.newCondition
import dev.wycey.mido.fraiselait.models.DeviceInformation
import dev.wycey.mido.fraiselait.models.DeviceState
import dev.wycey.mido.fraiselait.models.NonNullPinInformation
import dev.wycey.mido.fraiselait.util.Disposable
import dev.wycey.mido.fraiselait.util.OperatingSystem
import dev.wycey.mido.fraiselait.util.PrePhase
import dev.wycey.mido.fraiselait.util.getOperatingSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import processing.core.PApplet
import processing.serial.Serial
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
public class SerialDevice
  @JvmOverloads
  constructor(
    parent: PApplet,
    public val serialRate: Int,
    private val portSelection: SerialPortSelection = SerialPortSelection.Automatic
  ) : Disposable,
    PrePhase {
    public companion object {
      @JvmStatic
      public fun list(): Array<String> = Serial.list()
    }

    private val lock = Mutex()
    private val runningCondition = lock.newCondition()

    public var id: String? = null
      private set

    public var pins: NonNullPinInformation? = null
      private set(value) {
        if (field == value) return

        field = value

        pinsListeners.forEach { it(value) }
      }

    private var _state = AtomicReference<DeviceState?>(null)
    public var state: DeviceState?
      get() = _state.get()
      private set(value) {
        if (_state.get() == value) return

        _state.set(value)

        stateListeners.forEach { it(value) }
      }

    internal inner class SerialProxy :
      PApplet(),
      SerialReceiver {
      internal val enableSerialEvent = AtomicBoolean(false)

      var waitForNewResponse: Boolean = true
      var response: UByte = 0u

      override fun serialEvent(serial: Serial) {
        try {
          if (serial.available() == 0 || !enableSerialEvent.get()) return

          if (waitForNewResponse) {
            val startingResponse = serial.read()

            if (startingResponse == -1) return

            response = startingResponse.toUByte()
            waitForNewResponse = false

            when (startingResponse.toUByte()) {
              0u.toUByte() -> {
                waitForNewResponse = true
              }

              RESPONSE_DATA_START -> {
                serial.buffer(1 + 4 + 4)
              }

              RESPONSE_RESERVED_ERROR -> {
                serial.buffer(1)
              }

              else -> {
                println("Unknown response: $startingResponse")

                waitForNewResponse = true
              }
            }

            return
          }

          waitForNewResponse = true

          when (response) {
            RESPONSE_DATA_START -> {
              val dataBytes = readBytesNonSuspend(serial, 1 + 4 + 4)
              val tactSwitch = dataBytes[0] > 0
              val lightStrength = ByteBuffer.wrap(dataBytes.copyOfRange(1, 5)).int
              val coreTemperature = ByteBuffer.wrap(dataBytes.copyOfRange(5, 9)).float

              state = DeviceState(tactSwitch, lightStrength, coreTemperature)
            }

            RESPONSE_RESERVED_ERROR -> {
              val code = readByteNonSuspend(serial).toInt()

              if (code == 0x00) {
                println("Error received: Deserialization error")
              } else if (code == 0x01) {
                println("Error received: pin collision")
              }
            }
          }

          serial.buffer(1)
        } catch (e: Exception) {
          println("Error in serial event:")
          e.printStackTrace()
        }
      }
    }

    public var port: String? = if (portSelection is SerialPortSelection.Manual) portSelection.port else null
      set(value) {
        if (value == null) {
          if (serial != null) disconnect(serial!!)

          serial = null

          field = null

          return
        }

        if (value !in list()) {
          throw IllegalArgumentException("Port $value is not available")
        }

        serial?.stop()
        serial = null

        field = value

        refreshSerial()
      }

    private val maxRetries = 10
    private var errorCatchRetries = 0

    private fun disconnect(serial: Serial) {
      // Need to send additional '\n' to ensure that the device recognizes the disconnect command
      try {
        proxy.enableSerialEvent.set(false)
        serial.write(COMMAND_DATA_GET_LOOP_OFF.toInt())
      } catch (_: RuntimeException) {
        // Ignore I/O exceptions
      }

      pins = null
      state = null
    }

    private fun readByteNonSuspend(serial: Serial): UByte {
      var data: Int

      do {
        data = serial.read()
      } while (data < 0)

      return data.toUByte()
    }

    private fun readBytesNonSuspend(
      serial: Serial,
      byteCount: Int
    ): ByteArray {
      var data = ByteArray(byteCount)

      while (serial.available() < byteCount) {
        Thread.sleep(1)
      }

      var res: Int

      do {
        res = serial.readBytes(data)
      } while (res == 0)

      return data
    }

    private suspend fun readBytes(
      serial: Serial,
      byteCount: Int,
      timeout: Duration = 100.milliseconds
    ): ByteArray {
      val serialRead =
        CoroutineScope(coroutineContext).async {
          readBytesNonSuspend(serial, byteCount)
        }

      return withTimeout(timeout) { serialRead.await() }
    }

    private fun establishSerialConnection(): Serial? {
      try {
        val actualPort = port ?: return null

        val serial = Serial(proxy, actualPort, serialRate)

        serial.buffer(1)

        println("SerialDevice: Established connection with port ${serial.port.portName}")

        negotiateConnection(serial)

        return serial
      } catch (e: RuntimeException) {
        if (e.message?.contains("Port not found") == true ||
          e.message?.contains("Port not opened") == true ||
          e.message?.contains("Port is busy") == true
        ) {
          port = null

          if (errorCatchRetries < maxRetries) {
            errorCatchRetries++

            Thread.sleep(100 * errorCatchRetries.toLong())

            return establishSerialConnection()
          }

          errorCatchRetries = 0
        }

        if (e.message?.startsWith("Error opening serial port") == true) {
          if (getOperatingSystem() == OperatingSystem.LINUX) {
            if (errorCatchRetries < maxRetries) {
              errorCatchRetries++

              Thread.sleep(100 * errorCatchRetries.toLong())

              return establishSerialConnection()
            }

            println("Error opening serial port, giving up")

            errorCatchRetries = 0
          }

          port = null
        }

        throw e
      }
    }

    private fun negotiateConnection(serial: Serial) {
      var retries = 0

      CoroutineScope(coroutineContext).launch retry@{
        while (true) {
          // If the device is still sending data, send disconnect command
          if (serial.available() > 0) {
            println("Device is still sending data, trying to disconnect")

            disconnect(serial)

            delay(40)

            serial.clear()

            retries++

            if (retries >= 3) {
              throw RuntimeException("Failed to disconnect from device")
            }

            continue // Retry negotiation
          }

          // Flush all data before sending the negotiation data
          serial.clear()

          serial.write(COMMAND_DEVICE_INFO_GET.toInt())

          val deviceInformation =
            try {
              DeviceInformation.fromData(readBytes(serial, 2 + 4 + 6, 2.seconds)) // version + deviceId + pins
            } catch (e: Exception) {
              serial.clear()

              println("Failed to deserialize device information, retrying negotiation")

              delay(2.seconds)

              retries++

              if (retries >= 3) {
                throw RuntimeException("Failed to negotiate with device").initCause(e)
              }

              continue
            }

          println("Device information: $deviceInformation")

          if (deviceInformation.version < PROTOCOL_VERSION.toInt()) {
            throw RuntimeException(
              "Incompatible protocol version: Expected $PROTOCOL_VERSION, got ${deviceInformation.version}"
            )
          }

          id = deviceInformation.deviceId
          pins = deviceInformation.pins.toNonNullPinInformation()

          serial.write(COMMAND_DATA_GET_LOOP_ON.toInt())

          proxy.enableSerialEvent.set(true)
          phase = SerialDevicePhase.RUNNING

          break
        }
      }
    }

    private val commandChannel = Channel<Command>(UNLIMITED)

    private val proxy = SerialProxy()
    private val onDisposeCallbacks = mutableListOf<() -> Unit>()
    private val stateListeners = mutableListOf<(DeviceState?) -> Unit>()
    private val phaseListeners = mutableListOf<(SerialDevicePhase) -> Unit>()
    private val pinsListeners = mutableListOf<(NonNullPinInformation?) -> Unit>()

    @Volatile
    public var phase: SerialDevicePhase = SerialDevicePhase.NEW
      private set(value) {
        field = value

        phaseListeners.forEach { it(value) }

        if (value == SerialDevicePhase.RUNNING) {
          runBlocking {
            lock.withLock {
              runningCondition.signalAll()
            }
          }
        }
      }

    private var serial: Serial?

    private val job = SupervisorJob()

    private val exceptionHandler =
      CoroutineExceptionHandler { _, throwable ->
        println("SerialDevice error: ${throwable.message}")

        throwable.printStackTrace()
      }

    private val coroutineContext = Dispatchers.IO + job + exceptionHandler

    init {
      if (portSelection is SerialPortSelection.Automatic) {
        DevicePortWatcher.start(parent)

        DevicePortWatcher.listen {
          if (port == null) {
            port = it.firstOrNull()
          }

          if (port != null && port !in it) {
            port = null
          }
        }
      }

      serial =
        run {
          val conn = establishSerialConnection()

          if (conn == null) {
            phase = SerialDevicePhase.PAUSED
          }

          conn
        }

      onDispose {
        commandChannel.trySend(Command.RESET)

        serial?.stop()
      }

      CoroutineScope(coroutineContext).launch {
        val buf = mutableListOf<Command>()

        for (command in commandChannel) {
          lock.withLock {
            runningCondition.awaitUntil { phase == SerialDevicePhase.RUNNING }
          }

          buf.add(command)

          try {
            if (commandChannel.isEmpty) {
              lock.withLock {
                if (serial != null && serial?.active() == true) {
                  val finalCommand = buf.reduce { acc, other -> acc.merge(other) }

                  serial?.write(finalCommand.toDataBytes())

                  if (finalCommand.pinChanges != null) {
                    pins = pins?.merge(finalCommand.pinChanges!!)
                  }

                  buf.clear()
                }
              }
            }
          } catch (e: NullPointerException) {
            throw e
          } catch (_: RuntimeException) {
            // Nothing happened
          }
        }
      }
    }

    @JvmOverloads
    public fun send(
      command: Command,
      buffered: Boolean = true
    ) {
      if (phase == SerialDevicePhase.DISPOSED) {
        throw IllegalStateException("Cannot send command to disposed SerialDevice")
      }

      runBlocking {
        if (buffered) {
          commandChannel.send(command)

          return@runBlocking
        }

        lock.withLock {
          val ok =
            runningCondition.awaitUntil(3.seconds) { phase == SerialDevicePhase.RUNNING }

          if (!ok) {
            throw IllegalStateException("SerialDevice was not running after 3 seconds")
          }

          serial?.write(command.toDataBytes())

          if (command.pinChanges != null) {
            pins = pins?.merge(command.pinChanges!!)
          }
        }
      }
    }

    public fun refreshSerial(): Unit =
      runBlocking {
        lock.withLock {
          if (serial != null && phase == SerialDevicePhase.RUNNING) {
            disconnect(serial!!)
          }

          serial?.stop()
          serial = null
          id = null
          pins = null

          serial = establishSerialConnection()

          if (serial == null) {
            phase = SerialDevicePhase.PAUSED
          }
        }
      }

    public fun addPhaseChangeListener(callback: (SerialDevicePhase) -> Unit) {
      phaseListeners.add(callback)
    }

    public fun removePhaseChangeListener(callback: (SerialDevicePhase) -> Unit) {
      phaseListeners.remove(callback)
    }

    public fun addPinsChangeListener(callback: (NonNullPinInformation?) -> Unit) {
      pinsListeners.add(callback)
    }

    public fun removePinsChangeListener(callback: (NonNullPinInformation?) -> Unit) {
      pinsListeners.remove(callback)
    }

    public fun addStateChangeListener(callback: (DeviceState?) -> Unit) {
      stateListeners.add(callback)
    }

    public fun removeStateChangeListener(callback: (DeviceState?) -> Unit) {
      stateListeners.remove(callback)
    }

    public fun onDispose(callback: () -> Unit) {
      onDisposeCallbacks.add(callback)
    }

    override fun pre() {
      serial?.pre()
    }

    override fun dispose() {
      if (serial != null) disconnect(serial!!)

      phase = SerialDevicePhase.DISPOSED

      commandChannel.close()
      onDisposeCallbacks.forEach { it() }

      serial?.dispose()
      job.cancel()
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as SerialDevice

      if (serialRate != other.serialRate) return false
      if (portSelection != other.portSelection) return false
      if (runningCondition != other.runningCondition) return false
      if (id != other.id) return false
      if (pins != other.pins) return false
      if (port != other.port) return false

      return true
    }

    override fun hashCode(): Int {
      var result = serialRate
      result = 31 * result + portSelection.hashCode()
      result = 31 * result + runningCondition.hashCode()
      result = 31 * result + (id?.hashCode() ?: 0)
      result = 31 * result + (pins?.hashCode() ?: 0)
      result = 31 * result + (port?.hashCode() ?: 0)
      return result
    }

    override fun toString(): String = "SerialDevice(id=$id,rate=$serialRate,phase=$phase,state=$state)"
  }
