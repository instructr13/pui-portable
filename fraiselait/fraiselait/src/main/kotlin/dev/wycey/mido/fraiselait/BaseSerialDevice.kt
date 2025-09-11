package dev.wycey.mido.fraiselait

import dev.wycey.mido.fraiselait.cobs.COBS
import dev.wycey.mido.fraiselait.csum.CRC16CCITT
import dev.wycey.mido.fraiselait.packet.Frame
import dev.wycey.mido.fraiselait.packet.Packet
import dev.wycey.mido.fraiselait.util.VariableByteBuffer
import jssc.SerialPort
import jssc.SerialPortException
import jssc.SerialPortList
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

public abstract class BaseSerialDevice(
  public val serialRate: Int,
  public val port: String
) {
  public companion object {
    private const val TYPE_DEBUG_ECHO: UShort = 0xFFFFu

    private const val MAX_CHUNK_SIZE: UShort = 44u // 64 - 5 (cobs) - 14 (header+crc) - 1 (delimiter)
    private const val MAX_FRAME_SIZE = 2048
    private val RX_FRAME_TIMEOUT = Duration.ofMillis(2000)

    public const val VERSION: Int = 410

    @JvmStatic
    public var enableDebugOutput: Boolean = false

    @JvmStatic
    public fun list(): Array<String> = SerialPortList.getPortNames()
  }

  private val frameTable = mutableMapOf<UInt, Frame>()
  private val readerRunning = AtomicBoolean(false)
  private var readerThread: Thread? = null
  private var scheduler: ScheduledExecutorService? = null

  private inner class ReaderThread : Thread() {
    private val rxBuffer = VariableByteBuffer(ByteOrder.LITTLE_ENDIAN)

    init {
      name = "SerialDevice-reader-$port"
      isDaemon = true
    }

    override fun run() {
      while (readerRunning.get()) {
        if (interrupted() || disposed) break

        try {
          val available = serial.inputBufferBytesCount

          if (available > 0) {
            receiveRawData(available)

            if (rxBuffer.size > 0) {
              processRXBuffer()
            }
          } else {
            // No data available, sleep a bit
            sleep(1) // 1ms sleep
          }
        } catch (e: InterruptedException) {
          break
        } catch (e: SerialPortException) {
          dispose()

          break
        } catch (e: Exception) {
          println("Error in listener thread: $e")

          e.printStackTrace()
        }
      }
    }

    private fun receiveRawData(avail: Int) {
      rxBuffer.reserve(rxBuffer.size + avail)

      rxBuffer.put(serial.readBytes(avail))
    }

    private fun processRXBuffer() {
      var buf = rxBuffer.array
      var idx: Int

      while (true) {
        idx = buf.indexOf(0)

        if (idx == -1) break

        if (idx == 0) {
          // Empty packet, just skip it
          rxBuffer.position(1)
          rxBuffer.compact()

          continue
        }

        val packetData = buf.copyOfRange(0, idx)
        val decoded = COBS.decode(packetData)

        if (decoded != null) {
          packetQueue.put(decoded)
        } else {
          debugLog("COBS decode error")
        }

        // Remove preprocessed chunk and delimiter
        buf = buf.copyOfRange(idx + 1, buf.size)
      }

      rxBuffer.clear()
      rxBuffer.put(buf)
    }
  }

  private val packetQueue = LinkedBlockingQueue<ByteArray>(1024)
  private val handlerRunning = AtomicBoolean(false)
  private var handlerThread: Thread? = null

  private inner class HandlerThread : Thread() {
    init {
      name = "SerialDevice-handler-$port"
      isDaemon = true
    }

    override fun run() {
      while (handlerRunning.get()) {
        if (interrupted() || disposed) break

        try {
          val data = packetQueue.poll(100, TimeUnit.MILLISECONDS) ?: continue

          handlePacket(data)
        } catch (e: InterruptedException) {
          break
        } catch (e: Exception) {
          println("Error in handler thread: $e")

          e.printStackTrace()
        }
      }
    }

    private fun handlePacket(data: ByteArray) {
      if (data.size < 12) {
        // Not enough data for even the smallest chunk
        // type(2) + frame_id(4) + total_chunks(2) + chunk_index(2) + payload_size(2)
        debugLog("Packet too small: ${data.size} bytes")

        return
      }

      val packetBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

      val type = packetBuffer.short.toUShort()
      val frameId = packetBuffer.int.toUInt()
      val totalChunks = packetBuffer.short.toUShort()
      val chunkIndex = packetBuffer.short.toUShort()
      val payloadSize = packetBuffer.short.toUShort()

      val expectedPayloadSize = packetBuffer.remaining() - 2 // minus CRC16

      if (payloadSize != expectedPayloadSize.toUShort()) {
        debugLog("Payload size mismatch: expected $payloadSize, got ${packetBuffer.remaining()}")

        return
      }

      if (payloadSize > MAX_CHUNK_SIZE) {
        debugLog("Payload size too large: $payloadSize")

        return
      }

      val data = ByteArray(payloadSize.toInt())

      packetBuffer.get(data)

      val crc16 = packetBuffer.short.toUShort()

      if (!CRC16CCITT.verify(data, crc16)) {
        debugLog("CRC16 mismatch")

        frameTable.remove(frameId)

        return
      }

      val frame =
        frameTable.getOrPut(frameId) {
          Frame(type, frameId, totalChunks)
        }

      if (frame.chunks[chunkIndex.toInt()].isNotEmpty()) {
        debugLog("Duplicate chunk: frame $frameId, chunk $chunkIndex")

        frameTable.remove(frameId)

        return
      }

      frame.chunks[chunkIndex.toInt()] = data
      frame.received.incrementAndGet()
      frame.lastUpdate = Instant.now()

      if (frame.received.get() == frame.totalChunks.toInt()) {
        // Frame complete
        val fullPayload = ByteArray(frame.chunks.sumOf { it.size })

        var offset = 0

        for (chunk in frame.chunks) {
          System.arraycopy(chunk, 0, fullPayload, offset, chunk.size)

          offset += chunk.size
        }

        if (fullPayload.size > MAX_FRAME_SIZE) {
          debugLog("Frame size too large: ${fullPayload.size}")

          frameTable.remove(frameId)

          return
        }

        if (frame.type == TYPE_DEBUG_ECHO) {
          // Debug Message
          val logMessage = String(fullPayload)

          debugLog("Device Log: $logMessage")

          frameTable.remove(frameId)

          return
        }

        onRecv(Packet(frame.type, fullPayload))

        frameTable.remove(frameId)
      }
    }
  }

  private val frameIdCounter = AtomicInteger()
  private val writeQueue = ConcurrentLinkedQueue<ByteArray>()
  private val writerRunning = AtomicBoolean(false)
  private val writerExecutor =
    Executors.newSingleThreadExecutor { r ->
      Thread(r, "SerialDevice-writer-$port").apply {
        isDaemon = true
      }
    }

  @Volatile
  public var available: Boolean = false

  private var serial = SerialPort(port)

  @Volatile
  private var disposed = false

  init {
    try {
      serial.openPort()
      serial.setParams(
        serialRate,
        SerialPort.DATABITS_8,
        SerialPort.STOPBITS_1,
        SerialPort.PARITY_NONE
      )
      serial.setDTR(false)

      debugLog("Opened serial port at $serialRate baud")

      addShutdownHook()

      start()
    } catch (e: Exception) {
      println("Failed to open serial port '$port': $e")

      e.printStackTrace()

      dispose()
    }
  }

  protected abstract fun onRecv(packet: Packet)

  public fun send(packet: Packet) {
    if (disposed) {
      throw IllegalStateException("Device is disposed")
    }

    if (!available) return

    sendFrame(
      packet.type,
      frameIdCounter.getAndUpdate { if (it == UInt.MAX_VALUE.toInt()) 0 else it + 1 }.toUInt(),
      packet.payload
    )
  }

  public open fun start() {
    if (disposed) {
      throw IllegalStateException("Device is disposed")
    }

    if (available) return

    serial.setDTR(true)
    startReaders()

    scheduler = Executors.newSingleThreadScheduledExecutor()

    scheduler!!.scheduleAtFixedRate(::cleanupStaleFrames, 1, 1, TimeUnit.SECONDS)

    available = true

    debugLog("Started communication")
  }

  public open fun stop() {
    if (disposed) {
      throw IllegalStateException("Device is disposed")
    }

    if (!available) return

    stopReaders()
    serial.setDTR(false)

    stopWriter()

    available = false

    scheduler?.shutdownNow()
    scheduler = null

    debugLog("Stopped communication")
  }

  public open fun dispose() {
    if (disposed) return

    disposed = true
    available = false

    serial.setDTR(false)
    stopReaders()

    stopWriter()

    scheduler?.shutdownNow()
    scheduler = null

    serial.closePort()

    debugLog("Disposed")
  }

  private fun cleanupStaleFrames() {
    val now = Instant.now()

    frameTable.entries.removeIf { (_, frame) ->
      val age = Duration.between(frame.lastUpdate, now)

      if (age > RX_FRAME_TIMEOUT) {
        debugLog("Frame timeout: frame ${frame.frameId}")

        true
      } else {
        false
      }
    }
  }

  private fun startReaders() {
    if (readerRunning.compareAndSet(false, true)) {
      readerThread = ReaderThread().apply { start() }
    }

    if (handlerRunning.compareAndSet(false, true)) {
      handlerThread = HandlerThread().apply { start() }
    }
  }

  private fun stopReaders() {
    readerRunning.set(false)
    readerThread?.interrupt()
    readerThread = null

    handlerRunning.set(false)
    handlerThread?.interrupt()
    handlerThread = null
  }

  private fun startWriter() {
    if (writerRunning.compareAndSet(false, true)) {
      writerExecutor.submit {
        try {
          while (true) {
            if (disposed) break

            val data = writeQueue.poll() ?: break

            if (!available) {
              writeQueue.add(data)

              break
            }

            if (!serial.writeBytes(data)) {
              debugLog("Failed to write data")

              writeQueue.add(data)

              break
            }
          }
        } finally {
          writerRunning.set(false)

          if (writeQueue.isNotEmpty()) {
            startWriter()
          }
        }
      }
    }
  }

  private fun stopWriter() {
    writerExecutor.shutdownNow()

    writerRunning.set(false)

    writeQueue.clear()
  }

  private fun sendChunk(
    type: UShort,
    frameId: UInt,
    totalChunks: UShort,
    chunkIndex: UShort,
    data: ByteArray
  ) {
    val buffer =
      ByteBuffer
        .allocate(2 + 4 + 2 + 2 + 2 + data.size + 2)
        .order(ByteOrder.LITTLE_ENDIAN)

    buffer.putShort(type.toShort())
    buffer.putInt(frameId.toInt())
    buffer.putShort(totalChunks.toShort())
    buffer.putShort(chunkIndex.toShort())
    buffer.putShort(data.size.toShort())

    buffer.put(data)

    val crc = CRC16CCITT.compute(data)

    buffer.putShort(crc.toShort())

    val rawData = buffer.array()
    val cobsData = COBS.encode(rawData)

    writeQueue.add(cobsData)

    startWriter()
  }

  private fun sendFrame(
    type: UShort,
    frameId: UInt,
    data: ByteArray
  ) {
    if (data.size > MAX_FRAME_SIZE) {
      throw IllegalArgumentException("Frame size too large: ${data.size}")
    }

    var totalChunks =
      (data.size + MAX_CHUNK_SIZE.toInt() - 1) / MAX_CHUNK_SIZE.toInt()

    if (totalChunks == 0) totalChunks = 1

    for (i in 0 until totalChunks) {
      val chunkData =
        data.copyOfRange(
          i * MAX_CHUNK_SIZE.toInt(),
          ((i + 1) * MAX_CHUNK_SIZE.toInt()).coerceAtMost(data.size)
        )

      sendChunk(type, frameId, totalChunks.toUShort(), i.toUShort(), chunkData)
    }
  }

  private fun addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(
      Thread {
        dispose()
      }
    )
  }

  protected open fun debugLog(message: String) {
    if (!enableDebugOutput) return

    println("[$port] DEBUG: $message")
  }
}
