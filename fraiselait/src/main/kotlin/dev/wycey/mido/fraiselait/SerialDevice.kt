package dev.wycey.mido.fraiselait

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import dev.wycey.mido.fraiselait.commands.Command
import dev.wycey.mido.fraiselait.state.FramboiseState
import dev.wycey.mido.fraiselait.state.StateManager
import dev.wycey.mido.fraiselait.util.Disposable
import dev.wycey.mido.fraiselait.util.OperatingSystem
import dev.wycey.mido.fraiselait.util.PrePhase
import dev.wycey.mido.fraiselait.util.getOperatingSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import processing.core.PApplet
import processing.serial.Serial
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class SerialDevice
  @JvmOverloads
  constructor(
    parent: PApplet,
    private val serialRate: Int,
    initialPortName: String? = null
  ) : Disposable, PrePhase {
    companion object {
      private val mapperThreadLocal = ThreadLocal<ObjectMapper>()

      @JvmStatic
      fun list(): Array<String> = Serial.list()
    }

    private val maxDeserializationRetries = 10

    inner class SerialProxy : PApplet(), SerialReceiver {
      val retries = AtomicInteger(0)

      override fun serialEvent(serial: Serial) {
        if (phase != SerialDevicePhase.RUNNING) {
          return
        }

        if (serial.available() > 0) {
          val line =
            CoroutineScope(coroutineContext).async {
              withContext(coroutineContext) {
                serial.readStringUntil('\n'.code)
              }
            }

          CoroutineScope(coroutineContext).launch {
            val body = line.await() ?: return@launch

            if (body.startsWith("deserialization failed: ")) {
              println("deserialization error: ${body.substringAfter("deserialization failed: ")}")

              return@launch
            }

            launch(
              Dispatchers.Unconfined +
                mapperThreadLocal.asContextElement(
                  value =
                    jacksonMapperBuilder().enable(
                      StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
                    ).build()
                )
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
        if (port != null) {
          return Serial(proxy, port, serialRate)
        }

        val portName = Serial.list().firstOrNull() ?: return null

        return Serial(proxy, portName, serialRate)
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

          phase = if (conn != null) SerialDevicePhase.RUNNING else SerialDevicePhase.PAUSED

          conn
        }

      onDispose {
        commandChannel.trySend(Command.RESET)

        serial?.stop()
      }

      CoroutineScope(coroutineContext).launch {
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

                  serial?.write(finalCommand.toDataBytes())

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
        commandChannel.send(command)
      }

    fun refreshSerial() {
      synchronized(this) {
        serial?.stop()
        serial = null

        serial = establishSerialConnection()
      }

      phase = if (serial != null) SerialDevicePhase.RUNNING else SerialDevicePhase.PAUSED
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
