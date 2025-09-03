package dev.wycey.mido.fraiselait.builtins

import dev.wycey.mido.fraiselait.SerialDevice
import dev.wycey.mido.fraiselait.SerialPortSelection
import dev.wycey.mido.fraiselait.builtins.commands.Command
import dev.wycey.mido.fraiselait.capability.BaseCapability
import dev.wycey.mido.fraiselait.util.VariableByteBuffer
import java.nio.ByteBuffer
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
    serialRate: Int,
    portSelection: SerialPortSelection = SerialPortSelection.FirstAvailable,
    hostCapabilities: List<BaseCapability> = listOf(),
    connectImmediately: Boolean = true
  ) : SerialDevice(
      serialRate,
      portSelection,
      hostCapabilities,
      false
    ) {
    private companion object {
      const val COMMAND_DATA_GET_IMMEDIATE: UShort = 0x0090u
      const val COMMAND_DATA_GET_LOOP_OFF: UShort = 0x0092u
      const val COMMAND_DATA_GET_LOOP_ON: UShort = 0x0093u
      const val COMMAND_DATA_SET: UShort = 0x00E0u

      const val RESPONSE_DATA_SEND: UShort = 0x00F0u
    }

    private val atomicState: AtomicReference<FraiselaitDeviceState?> =
      AtomicReference(null)

    public val state: FraiselaitDeviceState?
      get() = atomicState.get()

    public var retrieveStateForever: Boolean = false
      set(value) {
        field = value

        if (value) {
          sendData(COMMAND_DATA_GET_LOOP_ON)
        } else {
          sendData(COMMAND_DATA_GET_LOOP_OFF)
        }
      }

    init {
      addCapability(FraiselaitDeviceCapability())

      onData(RESPONSE_DATA_SEND) {
        val newState = FraiselaitDeviceState()

        if (!newState.deserialize(it)) {
          return@onData
        }

        atomicState.set(newState)
      }

      if (connectImmediately) {
        connect()
      }
    }

    public fun requestState() {
      sendData(COMMAND_DATA_GET_IMMEDIATE)
    }

    public fun sendCommand(command: Command) {
      sendData(COMMAND_DATA_SET, command)
    }
  }
