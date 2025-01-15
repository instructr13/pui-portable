package dev.wycey.mido.fraiselait.state

import dev.wycey.mido.fraiselait.SerialDevice
import dev.wycey.mido.fraiselait.models.DeviceState

public object StateManager {
  internal var registeredDevice: SerialDevice? = null
    set(value) {
      field?.removeStateChangeListener(::markStateChange)

      field = value

      value?.addStateChangeListener(::markStateChange)
    }

  private val stateChangeListeners = mutableListOf<(DeviceState?) -> Unit>()

  public var state: DeviceState? = null
    private set

  private fun markStateChange(newState: DeviceState?) {
    state = newState

    stateChangeListeners.forEach { it(newState) }
  }

  public fun addStateChangeListener(listener: (DeviceState?) -> Unit) {
    stateChangeListeners.add(listener)
  }
}
