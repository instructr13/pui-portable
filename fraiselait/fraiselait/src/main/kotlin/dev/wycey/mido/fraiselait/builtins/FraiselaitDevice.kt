package dev.wycey.mido.fraiselait.builtins

import dev.wycey.mido.fraiselait.BaseSerialDevice
import dev.wycey.mido.fraiselait.builtins.DevicePortWatcher.addShutdownHook
import dev.wycey.mido.fraiselait.builtins.capability.BaseCapability
import dev.wycey.mido.fraiselait.builtins.commands.Command
import dev.wycey.mido.fraiselait.builtins.models.Serializable
import dev.wycey.mido.fraiselait.packet.Packet
import dev.wycey.mido.fraiselait.packet.ReservedErrorCode
import dev.wycey.mido.fraiselait.util.VariableByteBuffer
import jssc.SerialPortException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference

internal class FraiselaitDeviceCapability : BaseCapability {
  override val id: Short = 0x0040
  override val minSize: Int
    get() = 0

  override fun serialize(buffer: VariableByteBuffer) {
  }

  override fun deserialize(data: ByteBuffer): Boolean = true
}

public class FraiselaitDevice
  @JvmOverloads
  constructor(
    public val serialRate: Int,
    private val portSelection: SerialPortSelection = SerialPortSelection.FirstAvailable,
    hostCapabilities: List<BaseCapability> = listOf()
  ) {
    public companion object {
      public const val VERSION: Int = 410

      private const val COMMAND_DATA_GET_IMMEDIATE: UShort = 0x0090u
      private const val COMMAND_DATA_GET_LOOP_OFF: UShort = 0x0092u
      private const val COMMAND_DATA_GET_LOOP_ON: UShort = 0x0093u
      private const val COMMAND_DATA_SET: UShort = 0x00E0u

      private const val RESPONSE_DATA_SEND: UShort = 0x00F0u
    }

    private val onStatusChangeCallbacks = mutableListOf<(ConnectionStatus) -> Unit>()
    private val onDisposeCallbacks = mutableListOf<() -> Unit>()
    private val dataCallbacks = mutableListOf<Pair<UShort, (ByteBuffer) -> Unit>>()
    private val errorCallbacks = mutableListOf<Pair<UShort, (ByteBuffer) -> Unit>>()

    private val hostCapabilities = hostCapabilities.toMutableList()
    private val backingDeviceCapabilities = mutableListOf<BaseCapability>()

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

    @Volatile
    public var id: String? = null
      private set

    internal inner class FraiselaitSerialDevice(
      serialRate: Int,
      port: String
    ) : BaseSerialDevice(serialRate, port) {
      override fun onRecv(packet: Packet) {
        val type = PacketType.fromCode(packet.type)

        if (type == null) {
          debugLog("Received unknown packet type: 0x${packet.type.toString(16).padStart(4, '0')}")

          sendError(ReservedErrorCode.UNKNOWN_PACKET_TYPE.code)

          return
        }

        if (type == PacketType.ERROR) {
          val buffer = ByteBuffer.wrap(packet.payload).order(ByteOrder.LITTLE_ENDIAN)

          if (buffer.remaining() < 2) {
            debugLog("Received malformed error packet")

            sendError(ReservedErrorCode.MALFORMED_PACKET.code)

            return
          }

          val errorCode = buffer.short.toUShort()
          val errorData = ByteArray(buffer.remaining())

          buffer.get(errorData)

          val reservedError = ReservedErrorCode.fromCode(errorCode)

          if (reservedError != null) {
            debugLog("Received reserved error code: $reservedError")
          }

          errorCallbacks
            .filter {
              it.first == errorCode
            }.forEach { it.second(ByteBuffer.wrap(errorData).order(ByteOrder.LITTLE_ENDIAN)) }

          return
        }

        if (status == ConnectionStatus.CONNECTING) {
          processHandshake(type, packet.payload)?.let {
            sendError(it.code)

            throw IllegalStateException("Handshake failed: $it")
          } ?: run {
            status = ConnectionStatus.CONNECTED

            debugLog("Connected")
          }

          return
        }

        if (type == PacketType.DATA) {
          val buffer = ByteBuffer.wrap(packet.payload).order(ByteOrder.LITTLE_ENDIAN)

          if (buffer.remaining() < 2) {
            debugLog("Received malformed data packet")

            sendError(ReservedErrorCode.MALFORMED_PACKET.code)

            return
          }

          val dataType = buffer.short.toUShort()
          val data = ByteArray(buffer.remaining())

          buffer.get(data)

          dataCallbacks
            .filter {
              it.first == dataType
            }.forEach { it.second(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)) }

          return
        }
      }

      override fun stop() {
        super.stop()

        status = ConnectionStatus.NOT_CONNECTED
      }

      override fun dispose() {
        super.dispose()

        status = ConnectionStatus.DISPOSED
      }

      override fun debugLog(message: String) {
        val prefix =
          if (id != null) {
            "[$port | $id] "
          } else {
            "[$port]"
          }

        println("$prefix $message")
      }

      fun sendData(
        dataType: UShort,
        data: ByteArray = byteArrayOf()
      ) {
        if (status != ConnectionStatus.CONNECTED) return

        val payload = VariableByteBuffer(ByteOrder.LITTLE_ENDIAN)

        payload.putShort(dataType.toShort())
        payload.put(data)

        send(Packet(PacketType.DATA.code, payload.array))
      }

      @JvmOverloads
      fun sendData(
        dataType: Int,
        data: ByteArray = byteArrayOf()
      ) {
        sendData(dataType.toUShort(), data)
      }

      fun sendData(
        dataType: UShort,
        data: Serializable
      ) {
        sendData(dataType, data.toByteArray())
      }

      fun sendData(
        dataType: Int,
        data: Serializable
      ) {
        sendData(dataType.toUShort(), data)
      }

      fun sendError(
        errorCode: UShort,
        errorData: ByteArray = byteArrayOf()
      ) {
        debugLog("Sending error with code: 0x${errorCode.toString(16).padStart(4, '0')}")

        val payload = VariableByteBuffer(ByteOrder.LITTLE_ENDIAN)

        payload.putShort(errorCode.toShort())
        payload.put(errorData)

        send(Packet(PacketType.ERROR.code, payload.array))
      }

      fun sendError(
        errorCode: Int,
        errorData: ByteArray = byteArrayOf()
      ) {
        sendError(errorCode.toUShort(), errorData)
      }

      fun sendError(
        errorCode: UShort,
        errorData: Serializable
      ) {
        sendError(errorCode, errorData.toByteArray())
      }

      fun sendError(
        errorCode: Int,
        errorData: Serializable
      ) {
        sendError(errorCode.toUShort(), errorData)
      }

      fun sendHostHello() {
        val payload = VariableByteBuffer(ByteOrder.LITTLE_ENDIAN)

        payload.putShort(VERSION.toShort())
        payload.put(hostCapabilities.size.toByte())

        hostCapabilities.forEach {
          payload.putShort(it.id)

          val serializedCapability = it.toByteArray()

          payload.putShort(serializedCapability.size.toShort())
          payload.put(it.toByteArray())
        }

        debugLog("Sending Host Hello with ${hostCapabilities.size} capabilities")

        send(Packet(PacketType.HOST_HELLO.code, payload.array))
      }

      private fun processHandshake(
        type: PacketType,
        payload: ByteArray
      ): ReservedErrorCode? {
        if (type != PacketType.DEVICE_HELLO) return ReservedErrorCode.HANDSHAKE_NOT_COMPLETED

        if (!processDeviceHello(payload)) return ReservedErrorCode.MISSING_CAPABILITIES

        sendHostAck()

        return null
      }

      private fun processDeviceHello(payload: ByteArray): Boolean {
        val payload = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

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

      private fun sendHostAck() {
        debugLog("Sending Host Ack")

        send(Packet(PacketType.HOST_ACK.code))
      }
    }

    public val deviceCapabilities: List<BaseCapability>
      get() = backingDeviceCapabilities.toList()

    private var serial: FraiselaitSerialDevice? = null

    public var port: String? = if (portSelection is SerialPortSelection.Manual) portSelection.port else null
      set(value) {
        if (value == field) return

        disconnect()

        if (value != null && value !in BaseSerialDevice.list()) {
          throw IllegalArgumentException("Port '$value' not found")
        }

        try {
          serial?.dispose()
        } catch (_: SerialPortException) {
        }

        debugLog("Changing port to '$value'")

        serial = null
        field = value

        if (value != null) {
          connect()
        }
      }

    private val atomicState: AtomicReference<FraiselaitDeviceState?> =
      AtomicReference(null)

    public val state: FraiselaitDeviceState?
      get() = atomicState.get()

    public var retrieveStateForever: Boolean = false
      set(value) {
        field = value

        if (value) {
          serial?.sendData(COMMAND_DATA_GET_LOOP_ON)
        } else {
          serial?.sendData(COMMAND_DATA_GET_LOOP_OFF)
        }
      }

    init {
      if (portSelection is SerialPortSelection.FirstAvailable) {
        DevicePortWatcher.start()

        DevicePortWatcher.listen(::serialPortListener)
      }

      addShutdownHook()

      addCapability(FraiselaitDeviceCapability())

      onData(RESPONSE_DATA_SEND) {
        val newState = FraiselaitDeviceState()

        if (!newState.deserialize(it)) {
          return@onData
        }

        atomicState.set(newState)
      }

      connect()
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

    public fun requestState() {
      serial?.sendData(COMMAND_DATA_GET_IMMEDIATE)
    }

    public fun sendCommand(command: Command) {
      serial?.sendData(COMMAND_DATA_SET, command)
    }

    public fun connect() {
      if (status == ConnectionStatus.CONNECTED || status == ConnectionStatus.CONNECTING) {
        return
      }

      if (serial == null) {
        serial = FraiselaitSerialDevice(serialRate, port ?: throw IllegalStateException("Port not selected"))

        debugLog("Created serial device on port '$port'")

        status = ConnectionStatus.NOT_CONNECTED
      }

      serial?.start()
      serial?.sendHostHello()

      status = ConnectionStatus.CONNECTING
    }

    public fun disconnect() {
      serial?.stop()

      id = null

      debugLog("Disconnected")
    }

    public fun dispose() {
      if (status == ConnectionStatus.DISPOSED) {
        return
      }

      DevicePortWatcher.unlisten(::serialPortListener)

      serial?.dispose()

      onStatusChangeCallbacks.clear()
      onDisposeCallbacks.clear()

      id = null
      serial = null

      debugLog("Disposed")
    }

    private fun serialPortListener(serials: Array<String>) {
      if (port != null && port !in serials) {
        port = null
      }

      if (port == null && portSelection is SerialPortSelection.FirstAvailable) {
        port = serials.firstOrNull()
      }
    }

    private fun debugLog(message: String) {
      val prefix =
        if (id != null) {
          "[$port | $id] "
        } else {
          "[$port]"
        }

      println("$prefix $message")
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is FraiselaitDevice) return false

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
