package dev.wycey.mido.pui.bridges

internal interface DisplayBridgeContract

public class DisplayBridge internal constructor(
  private val eventHandlingBridge: EventHandlingBridge
) : BridgeBase(),
  DisplayBridgeContract,
  EventHandlingBridgeContract by eventHandlingBridge {
  public companion object {
    @JvmField
    internal var instanceNullable: DisplayBridge? = null

    @JvmStatic
    public val instance: DisplayBridge get() = checkInstance(instanceNullable)
  }

  override fun initInstance() {
    eventHandlingBridge.initInstance()
    instanceNullable = this

    println("Display bridge initialized")
  }
}
