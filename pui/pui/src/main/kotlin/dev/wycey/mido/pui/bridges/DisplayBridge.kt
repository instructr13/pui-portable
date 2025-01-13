package dev.wycey.mido.pui.bridges

interface DisplayBridgeContract

class DisplayBridge internal constructor(
  private val eventHandlingBridge: EventHandlingBridge
) : DisplayBridgeContract,
  EventHandlingBridgeContract by eventHandlingBridge,
  BridgeBase() {
  companion object {
    @JvmField
    var instanceNullable: DisplayBridge? = null

    @JvmStatic
    val instance get() = checkInstance(instanceNullable)
  }

  override fun initInstance() {
    eventHandlingBridge.initInstance()
    instanceNullable = this

    println("Display bridge initialized")
  }
}
