package dev.wycey.mido.fraiselait

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import dev.wycey.mido.fraiselait.commands.Command
import dev.wycey.mido.fraiselait.constants.MAGIC_COMMAND_NEGOTIATE
import dev.wycey.mido.fraiselait.constants.PROTOCOL_VERSION
import dev.wycey.mido.fraiselait.models.*
import dev.wycey.mido.fraiselait.state.FramboiseState
import dev.wycey.mido.fraiselait.state.StateManager
import dev.wycey.mido.fraiselait.util.Disposable
import dev.wycey.mido.fraiselait.util.OperatingSystem
import dev.wycey.mido.fraiselait.util.PrePhase
import dev.wycey.mido.fraiselait.util.getOperatingSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import org.msgpack.jackson.dataformat.MessagePackMapper
import processing.core.PApplet
import processing.serial.Serial
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class SerialDevice
  @JvmOverloads
  constructor(
    parent: PApplet,
    private val serialRate: Int,
    initialPortName: String? = null,
    val transferMode: TransferMode = TransferMode.MSGPACK,
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

              TransferMode.MSGPACK -> MessagePackMapper().handleBigIntegerAndBigDecimalAsString()
            }
        )
    }

    constructor(
      parent: PApplet,
      serialRate: Int,
      initialPortName: String? = null,
      transferMode: TransferMode = TransferMode.MSGPACK,
      additionalPinInformation: JVMPinInformation = JVMPinInformation()
    ) : this(parent, serialRate, initialPortName, transferMode, additionalPinInformation.toPinInformation())

    private val maxDeserializationRetries = 10

    var id: String? = null
      private set

    inner class SerialProxy : PApplet(), SerialReceiver {
      private val retries = AtomicInteger(0)

      override fun serialEvent(serial: Serial) {
        if (phase != SerialDevicePhase.RUNNING) {
          return
        }

        if (serial.available() > 0) {
          val line =
            CoroutineScope(coroutineContext).async {
              withContext(coroutineContext) {
                serial.readBytes()
              }
            }

          CoroutineScope(coroutineContext).launch {
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
                StateManager.state = mapper.readValue(body, FramboiseState::class.java)
              } catch (e: Exception) {
                when (e) {
                  is JsonParseException, is MismatchedInputException -> {
                    if (retries.incrementAndGet() < maxDeserializationRetries) {
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

    var port = initialPortName
      set(value) {
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

    private fun establishSerialConnection(): Serial? {
      try {
        val serial =
          if (port != null) {
            Serial(proxy, port, serialRate)
          } else {
            val portName = Serial.list().firstOrNull() ?: return null

            Serial(proxy, portName, serialRate)
          }

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

            Thread.sleep(100)

            return establishSerialConnection()
          }

          errorCatchRetries = 0
        }

        if (e.message?.startsWith("Error opening serial port") == true) {
          if (getOperatingSystem() == OperatingSystem.LINUX) {
            if (errorCatchRetries < maxRetries) {
              errorCatchRetries++

              Thread.sleep(100)

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

        serial.write(MAGIC_COMMAND_NEGOTIATE.toString().toByteArray() + mapper.writeValueAsBytes(data))

        println("Negotiating connection: ${serial.available()}")

        val deviceInformationBytes = serial.readBytesUntil('\n'.code) ?: throw RuntimeException("No data received")

        if (deviceInformationBytes[0] == 'E'.code.toByte()) {
          val errorBytes = deviceInformationBytes.copyOfRange(1, deviceInformationBytes.size)
          val errorData = mapper.readValue(errorBytes, ErrorData::class.java)

          throw RuntimeException("Error during negotiation (code ${errorData.code}): ${errorData.message}")
        }

        val deviceInformation = mapper.readValue(deviceInformationBytes, DeviceInformation::class.java)

        if (deviceInformation.version < PROTOCOL_VERSION) {
          val errorData =
            ErrorData(
              2u,
              "Negotiation failed: Incompatible protocol version: Expected $PROTOCOL_VERSION, got ${deviceInformation.version}"
            )

          serial.write(errorData.toDataBytes(mapper))

          throw RuntimeException(
            "Incompatible protocol version: Expected $PROTOCOL_VERSION, got ${deviceInformation.version}"
          )
        }

        id = deviceInformation.deviceId

        // Send acknoledgement
        serial.write('A'.code)

        phase = SerialDevicePhase.RUNNING
      }
    }

    private val commandChannel = Channel<Command>(UNLIMITED)

    private val proxy = SerialProxy()
    private val onDisposeCallbacks = mutableListOf<() -> Unit>()

    @Volatile
    var phase = SerialDevicePhase.NEW

    private var serial: Serial?

    private val job = SupervisorJob()

    private val exceptionHandler =
      CoroutineExceptionHandler { _, throwable ->
        println("SerialDevice error: ${throwable.message}")

        throwable.printStackTrace()
      }

    val coroutineContext = Dispatchers.IO + job + exceptionHandler

    init {
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
          while (phase != SerialDevicePhase.RUNNING) {
            yield()
          }

          buf.add(command)

          try {
            if (commandChannel.isEmpty) {
              synchronized(this) {
                if (serial != null && serial?.active() == true) {
                  val finalCommand = buf.reduce { acc, other -> acc.merge(other) }

                  serial?.write(finalCommand.toDataBytes(mapper, id!!))

                  buf.clear()
                }
              }

              delay(50)
            }
          } catch (e: RuntimeException) {
            // Nothing happened
          }
        }
      }

      parent.registerMethod("pre", this)
      parent.registerMethod("dispose", this)
    }

    fun send(command: Command) =
      runBlocking {
        if (phase == SerialDevicePhase.DISPOSED) {
          throw IllegalStateException("Cannot send command to disposed SerialDevice")
        }

        commandChannel.send(command)
      }

    fun refreshSerial() {
      synchronized(this) {
        serial?.stop()
        serial = null
        id = null

        serial = establishSerialConnection()

        if (serial == null) {
          phase = SerialDevicePhase.PAUSED
        }
      }
    }

    fun onDispose(callback: () -> Unit) {
      onDisposeCallbacks.add(callback)
    }

    override fun pre() {
      serial?.pre()
    }

    override fun dispose() {
      phase = SerialDevicePhase.DISPOSED

      commandChannel.close()
      onDisposeCallbacks.forEach { it() }
      serial?.dispose()
      job.cancel()
    }
  }
