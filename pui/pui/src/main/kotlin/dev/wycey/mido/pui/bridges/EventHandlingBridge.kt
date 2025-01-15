package dev.wycey.mido.pui.bridges

import dev.wycey.mido.pui.events.EventDelegate
import dev.wycey.mido.pui.events.key.KeyEventArgs
import dev.wycey.mido.pui.events.key.KeyEventType
import dev.wycey.mido.pui.events.mouse.MouseActions
import dev.wycey.mido.pui.events.mouse.toEventArgs
import processing.event.KeyEvent
import processing.event.MouseEvent

internal interface ProcessingEventHandler {
  fun mouseEvent(data: MouseEvent)

  fun keyEvent(data: KeyEvent)
}

internal interface EventHandlingBridgeContract {
  fun registerEvents()
}

public class EventHandlingBridge internal constructor(
  private val mouseEventBridge: MouseEventBridge,
  private val frameEventBridge: FrameEventBridge
) : EventHandlingBridgeContract,
  MouseEventBridgeContract by mouseEventBridge,
  FrameEventBridgeContract by frameEventBridge,
  ProcessingEventHandler,
  BridgeBase() {
  public companion object {
    @JvmField
    internal var instanceNullable: EventHandlingBridge? = null

    @JvmStatic
    public val instance: EventHandlingBridge get() = checkInstance(instanceNullable)
  }

  override fun initInstance() {
    frameEventBridge.initInstance()
    instanceNullable = this

    println("Event handling bridge initialized")

    registerEvents()
  }

  override fun registerEvents() {
    applet.registerMethod("mouseEvent", this)
    applet.registerMethod("keyEvent", this)
  }

  override fun mouseEvent(data: MouseEvent) {
    val e = data.toEventArgs(applet.pmouseX, applet.pmouseY)

    if (e.action == MouseActions.WHEEL) {
      handleMouseWheel(e)

      return
    }

    mouseEventBridge.translateMouseEvent(e)
  }

  private val globalKeyEventsDelegate = EventDelegate<(e: KeyEventArgs, type: KeyEventType) -> Unit>()

  private fun invokeGlobalKeyEvent(
    e: KeyEventArgs,
    type: KeyEventType
  ) {
    globalKeyEventsDelegate.forEach { it(e, type) }
  }

  private fun handleKeyEvent(
    e: KeyEventArgs,
    type: KeyEventType
  ) {
    invokeGlobalKeyEvent(e, type)
  }

  override fun keyEvent(data: KeyEvent) {
    val e = KeyEventArgs.fromKeyEvent(data)

    when (data.action) {
      KeyEvent.PRESS -> {
        handleKeyEvent(e, KeyEventType.Press)
      }

      KeyEvent.RELEASE -> {
        handleKeyEvent(e, KeyEventType.Release)
      }

      KeyEvent.TYPE -> {
        handleKeyEvent(e, KeyEventType.Type)
      }
    }
  }

  public fun addGlobalKeyEvent(event: (e: KeyEventArgs, type: KeyEventType) -> Unit) {
    globalKeyEventsDelegate += event
  }

  public fun removeGlobalKeyEvent(event: (e: KeyEventArgs, type: KeyEventType) -> Unit) {
    globalKeyEventsDelegate -= event
  }
}
