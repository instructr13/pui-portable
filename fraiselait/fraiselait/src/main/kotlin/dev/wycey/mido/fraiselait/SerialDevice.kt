package dev.wycey.mido.fraiselait

import dev.wycey.mido.fraiselait.capability.BaseCapability
import dev.wycey.mido.fraiselait.cobs.COBS
import dev.wycey.mido.fraiselait.models.Serializable
import dev.wycey.mido.fraiselait.packet.Packet
import dev.wycey.mido.fraiselait.packet.PacketType
import dev.wycey.mido.fraiselait.packet.ReservedErrorCode
import dev.wycey.mido.fraiselait.util.VariableByteBuffer
import jssc.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

public open class SerialDevice
  @JvmOverloads
  constructor(
    public val serialRate: Int,
    private val portSelection: SerialPortSelection = SerialPortSelection.FirstAvailable,
    hostCapabilities: List<BaseCapability> = listOf(),
    connectImmediately: Boolean = true
  ) {
    public companion object {
      public const val VERSION: Int = 400

      public val enableDebugOutput: Boolean = System.getenv("FRAISELAIT_DEBUG") == "1"

      @JvmStatic
      public fun list(): Array<String> = SerialPortList.getPortNames()
    }

    private val onStatusChangeCallbacks = mutableListOf<(ConnectionStatus) -> Unit>()
    private val onDisposeCallbacks = mutableListOf<() -> Unit>()
    private val dataCallbacks = mutableListOf<Pair<UShort, (ByteBuffer) -> Unit>>()
    private val errorCallbacks = mutableListOf<Pair<UShort, (ByteBuffer) -> Unit>>()

    @Volatile
    public var status: ConnectionStatus = ConnectionStatus.NOT_CONNECTED
      private set(value) {
        field = value

        onStatusChangeCallbacks.forEach { it(value) }

        if (value == ConnectionStatus.DISPOSED) {
          onDisposeCallbacks.forEach { it() }
        }

        if (value == ConnectionStatus.NOT_CONNECTED) {
          id = null
        }
      }

    private val hostCapabilities = hostCapabilities.toMutableList()
    private val backingDeviceCapabilities = mutableListOf<BaseCapability>()

    public val deviceCapabilities: List<BaseCapability>
      get() = backingDeviceCapabilities.toList()

    private inner class Listener : SerialPortEventListener {
      private val rxBuffer = VariableByteBuffer(ByteOrder.BIG_ENDIAN)
      private val rxCOBSBuffer = VariableByteBuffer(ByteOrder.BIG_ENDIAN)
      private var rxPacket: Packet? = null

      override fun serialEvent(e: SerialPortEvent?) {
        try {
          if (e == null || !e.isRXCHAR) return

          receiveToRXBuffer(e)

          val (cobsStatus, cobsArray) = COBS.decode(rxBuffer.array)

          rxBuffer.clear()

          rxCOBSBuffer.put(cobsArray)

          if (cobsStatus == COBS.DecodeStatus.ERROR) {
            rxCOBSBuffer.clear()

            debugLog("COBS decode error")

            return
          }

          if (cobsStatus == COBS.DecodeStatus.IN_PROGRESS) {
            return
          }

          rxCOBSBuffer.flip()

          rxPacket = Packet.parse(rxCOBSBuffer)

          rxCOBSBuffer.clear()

          if (rxPacket == null) {
            debugLog("Packet parse error")

            return
          }

          val type = rxPacket!!.type

          if (type == PacketType.ERROR) {
            val payload = ByteBuffer.wrap(rxPacket!!.payload).order(ByteOrder.BIG_ENDIAN)

            if (payload.remaining() < 2) {
              return
            }

            val buffer = payload.getShort().toUShort()

            ReservedErrorCode.fromCode(buffer)?.let {
              throw IllegalStateException("Received error from device: $it")
            }

            val errorData = ByteArray(payload.remaining())

            payload.get(errorData)

            errorCallbacks.filter { it.first == buffer }.forEach {
              it.second(ByteBuffer.wrap(errorData).order(ByteOrder.BIG_ENDIAN))
            }
          }

          if (status == ConnectionStatus.CONNECTING) {
            processHandshake()?.let {
              sendError(it.code)

              throw IllegalStateException("Handshake failed: $it")
            } ?: run {
              status = ConnectionStatus.CONNECTED

              debugLog("Connected")
            }

            return
          }

          when (type) {
            PacketType.DATA -> {
              val payload = ByteBuffer.wrap(rxPacket!!.payload).order(ByteOrder.BIG_ENDIAN)

              if (payload.remaining() < 2) {
                sendError(ReservedErrorCode.MALFORMED_PACKET.code)

                return
              }

              val dataType = payload.getShort().toUShort()

              val data = ByteArray(payload.remaining())

              payload.get(data)

              dataCallbacks.filter { it.first == dataType }.forEach {
                it.second(ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN))
              }
            }

            PacketType.DEBUG_ECHO -> {
              val payload = ByteBuffer.wrap(rxPacket!!.payload).order(ByteOrder.BIG_ENDIAN)
              val message = String(payload.array(), Charsets.UTF_8)

              debugLog(message)
            }

            else -> {
              sendError(ReservedErrorCode.UNKNOWN_PACKET_TYPE.code)
            }
          }
        } catch (e: Exception) {
          print("Error in serial event: ")

          e.printStackTrace()

          sendError(ReservedErrorCode.INTERNAL_ERROR.code)

          disconnect()
        }
      }

      private fun receiveToRXBuffer(e: SerialPortEvent) {
        val avail = e.eventValue

        rxBuffer.reserve(rxBuffer.size + avail)

        rxBuffer.put(serial!!.readBytes(avail))
      }

      private fun receiveDeviceHello(payload: ByteArray): Boolean {
        val payload = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)

        if (payload.remaining() < 5) {
          throw IllegalStateException("Device Hello payload too short")
        }

        val deviceId = payload.getInt()

        id = String.format("%08x", deviceId)

        val capabilityCount = payload.get().toInt() and 0xFF

        for (i in 0 until capabilityCount) {
          if (payload.remaining() < 4) {
            throw IllegalStateException("Device Hello payload too short for capability $i")
          }

          val capabilityId = payload.getShort()
          val capabilitySize = payload.getShort().toUShort().toInt()

          if (payload.remaining() < capabilitySize) {
            throw IllegalStateException("Device Hello payload too short for capability data $i")
          }

          val capabilityData = ByteArray(capabilitySize)

          payload.get(capabilityData)

          val capability = backingDeviceCapabilities.find { it.id == capabilityId } ?: continue

          if (!capability.fromByteArray(capabilityData)) {
            throw IllegalStateException("Failed to parse capability data for capability $i")
          }
        }

        debugLog("Received Device Hello with $capabilityCount capabilities")

        return true
      }

      private fun processHandshake(): ReservedErrorCode? {
        val packet = rxPacket ?: throw IllegalStateException("No packet to process")

        if (packet.type != PacketType.DEVICE_HELLO) return ReservedErrorCode.HANDSHAKE_NOT_COMPLETED

        if (!receiveDeviceHello(packet.payload)) return ReservedErrorCode.MISSING_CAPABILITIES

        sendHostAck()

        return null
      }
    }

    private var serial: SerialPort? = null

    public var port: String? = if (portSelection is SerialPortSelection.Manual) portSelection.port else null
      set(value) {
        if (value == field) return

        disconnect()

        if (value != null && value !in list()) {
          throw IllegalArgumentException("Port '$value' not found")
        }

        try {
          serial?.closePort()
        } catch (_: SerialPortException) {
        }

        debugLog("Changing port to '$value'")

        serial = null
        field = value

        if (value != null) {
          connect()
        }
      }

    public var id: String? = null
      private set

    init {
      if (portSelection is SerialPortSelection.FirstAvailable) {
        DevicePortWatcher.start()

        DevicePortWatcher.listen(::serialPortListener)
      }

      if (connectImmediately) {
        connect()
      }

      addShutdownHook()
    }

    public fun addCapability(
      hostCapability: BaseCapability,
      deviceCapabilityRef: BaseCapability? = null
    ) {
      if (status != ConnectionStatus.NOT_CONNECTED) {
        throw IllegalStateException("Cannot add capabilities after connecting")
      }

      if (hostCapabilities.any { it.id == hostCapability.id }) {
        throw IllegalArgumentException("Host capability with id ${hostCapability.id} already exists")
      }

      hostCapabilities.add(hostCapability)
      deviceCapabilityRef?.let {
        backingDeviceCapabilities.add(it)
      }
    }

    public fun onStatusChange(callback: (ConnectionStatus) -> Unit) {
      onStatusChangeCallbacks.add(callback)
    }

    public fun removeOnStatusChange(callback: (ConnectionStatus) -> Unit) {
      onStatusChangeCallbacks.remove(callback)
    }

    public fun onDispose(callback: () -> Unit) {
      onDisposeCallbacks.add(callback)
    }

    public fun removeOnDispose(callback: () -> Unit) {
      onDisposeCallbacks.remove(callback)
    }

    public fun onData(
      dataType: UShort,
      callback: (ByteBuffer) -> Unit
    ) {
      dataCallbacks.add(dataType to callback)
    }

    public fun removeOnData(
      dataType: UShort,
      callback: (ByteBuffer) -> Unit
    ) {
      dataCallbacks.remove(dataType to callback)
    }

    public fun onError(
      errorCode: UShort,
      callback: (ByteBuffer) -> Unit
    ) {
      errorCallbacks.add(errorCode to callback)
    }

    public fun removeOnError(
      errorCode: UShort,
      callback: (ByteBuffer) -> Unit
    ) {
      errorCallbacks.remove(errorCode to callback)
    }

    public fun sendData(
      dataType: UShort,
      data: ByteArray = byteArrayOf()
    ) {
      if (status != ConnectionStatus.CONNECTED) return

      val payload = VariableByteBuffer(ByteOrder.BIG_ENDIAN)

      payload.putShort(dataType.toShort())
      payload.put(data)

      sendPacket(Packet(PacketType.DATA, payload.array))
    }

    @JvmOverloads
    public fun sendData(
      dataType: Int,
      data: ByteArray = byteArrayOf()
    ) {
      sendData(dataType.toUShort(), data)
    }

    public fun sendData(
      dataType: UShort,
      data: Serializable
    ) {
      sendData(dataType, data.toByteArray())
    }

    public fun sendData(
      dataType: Int,
      data: Serializable
    ) {
      sendData(dataType.toUShort(), data)
    }

    public fun sendError(
      errorCode: UShort,
      errorData: ByteArray = byteArrayOf()
    ) {
      val payload = VariableByteBuffer(ByteOrder.BIG_ENDIAN)

      payload.putShort(errorCode.toShort())
      payload.put(errorData)

      sendPacket(Packet(PacketType.ERROR, payload.array))
    }

    @JvmOverloads
    public fun sendError(
      errorCode: Int,
      errorData: ByteArray = byteArrayOf()
    ) {
      sendError(errorCode.toUShort(), errorData)
    }

    public fun sendError(
      errorCode: UShort,
      errorData: Serializable
    ) {
      sendError(errorCode, errorData.toByteArray())
    }

    public fun sendError(
      errorCode: Int,
      errorData: Serializable
    ) {
      sendError(errorCode.toUShort(), errorData)
    }

    public fun connect() {
      if (status == ConnectionStatus.DISPOSED) {
        throw IllegalStateException("Device is disposed")
      }

      if (status == ConnectionStatus.CONNECTED || status == ConnectionStatus.CONNECTING) {
        return
      }

      if (serial == null) {
        serial = createSerial()

        if (serial == null) {
          status = ConnectionStatus.NOT_CONNECTED

          return
        }
      }

      debugLog("Connecting")

      serial?.addEventListener(Listener(), SerialPort.MASK_RXCHAR)
      serial?.setDTR(true)

      sendHostHello()

      status = ConnectionStatus.CONNECTING
    }

    public fun disconnect() {
      if (status == ConnectionStatus.DISPOSED) {
        throw IllegalStateException("Device is disposed")
      }

      serial?.setDTR(false)
      serial?.removeEventListener()

      status = ConnectionStatus.NOT_CONNECTED
      id = null

      debugLog("Disconnected")
    }

    public fun dispose() {
      if (status == ConnectionStatus.DISPOSED) {
        return
      }

      DevicePortWatcher.unlisten(::serialPortListener)

      status = ConnectionStatus.DISPOSED

      try {
        serial?.closePort()
      } catch (_: SerialPortException) {
      }

      onStatusChangeCallbacks.clear()
      onDisposeCallbacks.clear()

      id = null
      serial = null

      debugLog("Disposed")
    }

    private fun addShutdownHook() {
      Runtime.getRuntime().addShutdownHook(
        Thread {
          dispose()
        }
      )
    }

    private fun serialPortListener(serials: Array<String>) {
      if (port != null && port !in serials) {
        port = null
      }

      if (port == null && portSelection is SerialPortSelection.FirstAvailable) {
        port = serials.firstOrNull()
      }
    }

    private fun createSerial(): SerialPort? {
      val actualPort = port ?: return null
      val serial = SerialPort(actualPort)

      try {
        serial.openPort()
        serial.setParams(
          serialRate,
          SerialPort.DATABITS_8,
          SerialPort.STOPBITS_1,
          SerialPort.PARITY_NONE
        )
        serial.setDTR(false)

        debugLog("Opened serial port '$actualPort' at $serialRate baud")
      } catch (e: Exception) {
        println("Failed to open serial port '$actualPort': $e")

        e.printStackTrace()

        serial.closePort()

        status = ConnectionStatus.NOT_CONNECTED

        return null
      }

      return serial
    }

    private fun sendPacket(packet: Packet) {
      if (status == ConnectionStatus.DISPOSED) {
        throw IllegalStateException("Device is disposed")
      }

      debugLog("Sending packet: $packet")

      val rawData = packet.encode()
      val cobsData = COBS.encode(rawData)

      serial?.writeBytes(cobsData)
    }

    private fun sendHostHello() {
      val payload = VariableByteBuffer(ByteOrder.BIG_ENDIAN)

      payload.putShort(VERSION.toShort())
      payload.put(hostCapabilities.size.toByte())

      hostCapabilities.forEach {
        payload.putShort(it.id)

        val serializedCapability = it.toByteArray()

        payload.putShort(serializedCapability.size.toShort())
        payload.put(it.toByteArray())
      }

      debugLog("Sending Host Hello with ${hostCapabilities.size} capabilities")

      sendPacket(Packet(PacketType.HOST_HELLO, payload.array))
    }

    private fun sendHostAck() {
      debugLog("Sending Host Ack")

      sendPacket(Packet(PacketType.HOST_ACK))
    }

    private fun debugLog(message: String) {
      if (!enableDebugOutput) return

      val prefix = if (id != null) "[$id | $port]" else "[$port]"

      println("$prefix DEBUG: $message")
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is SerialDevice) return false

      if (serialRate != other.serialRate) return false
      if (status != other.status) return false
      if (port != other.port) return false
      if (id != other.id) return false

      return true
    }

    override fun hashCode(): Int {
      var result = serialRate

      result = 31 * result + status.hashCode()
      result = 31 * result + (port?.hashCode() ?: 0)
      result = 31 * result + (id?.hashCode() ?: 0)

      return result
    }

    override fun toString(): String = "SerialDevice(id=$id, port=$port, rate=$serialRate, status=$status)"
  }
