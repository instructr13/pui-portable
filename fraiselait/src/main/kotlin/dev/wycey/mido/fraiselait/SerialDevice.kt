package dev.wycey.mido.fraiselait

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.kotlinModule
import dev.wycey.mido.fraiselait.commands.Command
import dev.wycey.mido.fraiselait.constants.MAGIC_COMMAND_DISCONNECT
import dev.wycey.mido.fraiselait.constants.MAGIC_COMMAND_NEGOTIATE
import dev.wycey.mido.fraiselait.constants.PROTOCOL_VERSION
import dev.wycey.mido.fraiselait.coroutines.sync.ReadWriteMutex
import dev.wycey.mido.fraiselait.coroutines.sync.newCondition
import dev.wycey.mido.fraiselait.models.*
import dev.wycey.mido.fraiselait.state.StateManager
import dev.wycey.mido.fraiselait.util.Disposable
import dev.wycey.mido.fraiselait.util.OperatingSystem
import dev.wycey.mido.fraiselait.util.PrePhase
import dev.wycey.mido.fraiselait.util.getOperatingSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.msgpack.jackson.dataformat.MessagePackMapper
import processing.core.PApplet
import processing.serial.Serial
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SerialDevice
  @JvmOverloads
  constructor(
    parent: PApplet,
    private val serialRate: Int,
    private val portSelection: SerialPortSelection = SerialPortSelection.Automatic,
    var transferMode: TransferMode = TransferMode.MSGPACK,
    private val additionalPinInformation: PinInformation = PinInformation()
  ) : Disposable, PrePhase {
    companion object {
      private val mapperThreadLocal = ThreadLocal<ObjectMapper>()

      @JvmStatic
      fun list(): Array<String> = Serial.list()

      private fun getMapperContextElement(transferMode: TransferMode) =
        mapperThreadLocal.asContextElement(
          value =
            when (transferMode) {
              TransferMode.JSON ->
                jacksonMapperBuilder().enable(
                  StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
                ).build()

              TransferMode.MSGPACK ->
                MessagePackMapper().handleBigIntegerAndBigDecimalAsString().registerModule(
                  // Manually register kotlin module to avoid issues with class loader
                  kotlinModule {}
                )
            }
        )
    }

    constructor(
      parent: PApplet,
      serialRate: Int,
      portSelection: SerialPortSelection = SerialPortSelection.Automatic,
      transferMode: TransferMode = TransferMode.MSGPACK,
      additionalPinInformation: JVMPinInformation = JVMPinInformation()
    ) : this(parent, serialRate, portSelection, transferMode, additionalPinInformation.toPinInformation())

    private val lock = Mutex()
    private val runningCondition = lock.newCondition()
    private val maxDeserializationRetries = 10
    private val receivingDataQueue = ReceivingDataQueue()

    var id: String? = null
      private set

    var pins: NonNullPinInformation? = null
      private set(value) {
        if (field == value) return

        field = value

        pinsListeners.forEach { it(value) }
      }

    private val stateLock = ReadWriteMutex()

    var state: DeviceState? = null
      get() =
        runBlocking {
          stateLock.withReadLock {
            field
          }
        }
      private set(value) {
        if (field == value) return

        field = value

        stateListeners.forEach { it(value) }
      }

    inner class SerialProxy : PApplet(), SerialReceiver {
      private val retries = AtomicInteger(0)

      override fun serialEvent(serial: Serial) {
        if (serial.available() > 0) {
          val line =
            CoroutineScope(coroutineContext).async {
              withContext(coroutineContext) {
                val bytes = serial.readBytesUntil('\n'.code)

                if (bytes != null && bytes.size < 3) null else bytes
              }
            }

          CoroutineScope(coroutineContext).launch(
            getMapperContextElement(transferMode)
          ) {
            val body = line.await() ?: return@launch

            if (body[0] == 'E'.code.toByte()) {
              val errorBytes = body.copyOfRange(1, body.size)
              val errorData = mapperThreadLocal.get().readValue(errorBytes, ErrorData::class.java)

              println("Error received: ${errorData.message}")

              return@launch
            }

            launch(
              Dispatchers.Unconfined +
                getMapperContextElement(transferMode)
            ) retry@{
              val mapper = mapperThreadLocal.get()

              try {
                if (phase == SerialDevicePhase.RUNNING) {
                  stateLock.withWriteLock {
                    state = mapper.readValue(body, DeviceState::class.java)
                  }

                  retries.set(0)

                  return@retry
                }

                receivingDataQueue.push(body)
              } catch (e: Exception) {
                when (e) {
                  is JsonParseException, is MismatchedInputException -> {
                    val newRetries = retries.incrementAndGet()

                    if (newRetries < maxDeserializationRetries) {
                      delay(10 * newRetries)

                      return@retry
                    }

                    println("Deserialization failed, giving up")
                    retries.set(0)
                  }

                  else -> {
                    throw e
                  }
                }
              }
            }
          }
        }
      }
    }

    var port = if (portSelection is SerialPortSelection.Manual) portSelection.port else null
      set(value) {
        if (value == null) {
          disconnect()
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

    private fun disconnect() {
      // Need to send additional '\n' to ensure that the device recognizes the disconnect command
      try {
        serial?.write(MAGIC_COMMAND_DISCONNECT.toString() + "\n")
      } catch (e: RuntimeException) {
        // Ignore I/O exceptions
      }

      id = null
      pins = null
      state = null
    }

    private suspend fun flushData() {
      val serialRead =
        CoroutineScope(coroutineContext).async {
          serial?.clear()
        }

      while (!receivingDataQueue.isEmpty()) {
        receiveData()
      }

      serialRead.await()
    }

    private suspend fun receiveData() = receivingDataQueue.pop()

    private fun sendData(
      data: Int,
      serial: Serial? = null
    ) {
      (serial ?: this.serial)?.write(data)
    }

    private fun sendData(
      data: ByteArray,
      serial: Serial? = null
    ) {
      (serial ?: this.serial)?.write(data)
    }

    private fun establishSerialConnection(): Serial? {
      try {
        val actualPort = port ?: return null

        val serial = Serial(proxy, actualPort, serialRate)

        serial.bufferUntil('\n'.code)

        println("Established connection with port ${serial.port.portName}")

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
      val data = NegotiationData(additionalPinInformation)

      CoroutineScope(coroutineContext).launch(
        getMapperContextElement(transferMode)
      ) {
        val mapper = mapperThreadLocal.get()

        // Flush all data before sending the negotiation data
        flushData()

        // If the device is still sending data, send disconnect command
        if (serial.available() > 0) {
          disconnect()

          delay(40)

          flushData()
        }

        sendData(
          MAGIC_COMMAND_NEGOTIATE.toString().toByteArray() + transferMode.toByte() +
            mapper.writeValueAsBytes(
              data
            ),
          serial
        )

        val deviceInformationBytes = receiveData()

        if (deviceInformationBytes[0] == 'E'.code.toByte()) {
          val errorBytes = deviceInformationBytes.copyOfRange(1, deviceInformationBytes.size)
          val errorData = mapper.readValue(errorBytes, ErrorData::class.java)

          throw RuntimeException("Error during negotiation (code ${errorData.code}): ${errorData.message}")
        }

        val deviceInformation = mapper.readValue(deviceInformationBytes, DeviceInformation::class.java)

        println("New device: ${deviceInformation.deviceId}")

        if (deviceInformation.version < PROTOCOL_VERSION.toInt()) {
          val errorData =
            ErrorData(
              2,
              "Negotiation failed: Incompatible protocol version: Expected $PROTOCOL_VERSION, got ${deviceInformation.version}"
            )

          sendData(errorData.toDataBytes(mapper))

          throw RuntimeException(
            "Incompatible protocol version: Expected $PROTOCOL_VERSION, got ${deviceInformation.version}"
          )
        }

        id = deviceInformation.deviceId
        pins = deviceInformation.pins.toNonNullPinInformation()

        // Send acknowledgement
        sendData('A'.code, serial)

        phase = SerialDevicePhase.RUNNING
      }
    }

    private val commandChannel = Channel<Command>(UNLIMITED)

    private val proxy = SerialProxy()
    private val onDisposeCallbacks = mutableListOf<() -> Unit>()
    private val stateListeners = mutableListOf<(DeviceState?) -> Unit>()
    private val phaseListeners = mutableListOf<(SerialDevicePhase) -> Unit>()
    private val pinsListeners = mutableListOf<(NonNullPinInformation?) -> Unit>()

    @Volatile
    var phase = SerialDevicePhase.NEW
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

    val coroutineContext = Dispatchers.IO + job + exceptionHandler

    init {
      DevicePortWatcher.start(parent)

      if (StateManager.registeredDevice == null) {
        registerToStateManager()
      }

      if (portSelection is SerialPortSelection.Automatic) {
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

      CoroutineScope(coroutineContext).launch(
        getMapperContextElement(transferMode)
      ) {
        val mapper = mapperThreadLocal.get()
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

                  serial?.write(finalCommand.toDataBytes(mapper, id!!))

                  if (finalCommand.pinChanges != null) {
                    pins = pins?.merge(finalCommand.pinChanges!!)
                  }

                  buf.clear()
                }
              }
            }
          } catch (e: RuntimeException) {
            // Nothing happened
          }
        }
      }
    }

    fun send(command: Command) =
      runBlocking {
        if (phase == SerialDevicePhase.DISPOSED) {
          throw IllegalStateException("Cannot send command to disposed SerialDevice")
        }

        commandChannel.send(command)
      }

    fun sendDirect(command: Command) {
      if (phase == SerialDevicePhase.DISPOSED) {
        throw IllegalStateException("Cannot send command to disposed SerialDevice")
      }

      runBlocking {
        val ok =
          lock.withLock {
            runningCondition.awaitUntil(3.seconds) { phase == SerialDevicePhase.RUNNING }
          }

        if (!ok) {
          throw IllegalStateException("SerialDevice is not running after 3 seconds")
        }

        val mapper = mapperThreadLocal.get()

        serial?.write(command.toDataBytes(mapper, id!!))

        if (command.pinChanges != null) {
          pins = pins?.merge(command.pinChanges!!)
        }
      }
    }

    fun refreshSerial() =
      runBlocking {
        lock.withLock {
          if (serial != null && phase == SerialDevicePhase.RUNNING) {
            disconnect()
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

    fun addPhaseChangeListener(callback: (SerialDevicePhase) -> Unit) {
      phaseListeners.add(callback)
    }

    fun removePhaseChangeListener(callback: (SerialDevicePhase) -> Unit) {
      phaseListeners.remove(callback)
    }

    fun addPinsChangeListener(callback: (NonNullPinInformation?) -> Unit) {
      pinsListeners.add(callback)
    }

    fun removePinsChangeListener(callback: (NonNullPinInformation?) -> Unit) {
      pinsListeners.remove(callback)
    }

    fun addStateChangeListener(callback: (DeviceState?) -> Unit) {
      stateListeners.add(callback)
    }

    fun removeStateChangeListener(callback: (DeviceState) -> Unit) {
      stateListeners.remove(callback)
    }

    fun registerToStateManager() {
      StateManager.registeredDevice = this
    }

    fun onDispose(callback: () -> Unit) {
      onDisposeCallbacks.add(callback)
    }

    override fun pre() {
      serial?.pre()
    }

    override fun dispose() {
      disconnect()

      phase = SerialDevicePhase.DISPOSED

      commandChannel.close()
      onDisposeCallbacks.forEach { it() }

      runBlocking {
        receivingDataQueue.clear()
      }

      serial?.dispose()
      job.cancel()
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as SerialDevice

      if (serialRate != other.serialRate) return false
      if (portSelection != other.portSelection) return false
      if (transferMode != other.transferMode) return false
      if (additionalPinInformation != other.additionalPinInformation) return false
      if (runningCondition != other.runningCondition) return false
      if (id != other.id) return false
      if (pins != other.pins) return false
      if (port != other.port) return false

      return true
    }

    override fun hashCode(): Int {
      var result = serialRate
      result = 31 * result + portSelection.hashCode()
      result = 31 * result + transferMode.hashCode()
      result = 31 * result + additionalPinInformation.hashCode()
      result = 31 * result + runningCondition.hashCode()
      result = 31 * result + (id?.hashCode() ?: 0)
      result = 31 * result + (pins?.hashCode() ?: 0)
      result = 31 * result + (port?.hashCode() ?: 0)
      return result
    }
  }
