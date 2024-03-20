package dev.wycey.mido.pui.bridges

import processing.core.PApplet

class PUIBridge private constructor(
  private val mouseEventBridge: MouseEventBridge = MouseEventBridge(),
  private val frameEventBridge: FrameEventBridge = FrameEventBridge(),
  private val eventHandlingBridge: EventHandlingBridge =
    EventHandlingBridge(
      mouseEventBridge,
      frameEventBridge
    ),
  private val displayBridge: DisplayBridge =
    DisplayBridge(
      eventHandlingBridge
    ),
  private val rendererBridge: RendererBridge = RendererBridge(mouseEventBridge, frameEventBridge, displayBridge),
  private val componentsBridge: ComponentsBridge =
    ComponentsBridge(
      mouseEventBridge,
      frameEventBridge,
      rendererBridge
    )
) : BridgeBase(),
  MouseEventBridgeContract by mouseEventBridge,
  FrameEventBridgeContract by frameEventBridge,
  EventHandlingBridgeContract by eventHandlingBridge,
  DisplayBridgeContract by displayBridge,
  RendererBridgeContract by rendererBridge,
  ComponentsBridgeContract by componentsBridge {
  companion object {
    @JvmStatic
    fun init(applet: PApplet): ComponentsBridge {
      BridgeBase.applet = applet

      if (ComponentsBridge.instanceNullable == null) {
        PUIBridge()
      }

      return ComponentsBridge.instance
    }
  }

  override fun initInstance() {
    componentsBridge.initInstance()

    onPersistentDraw(::drawFrame)
  }

  init {
    initInstance()
  }

  override fun drawFrame() {
    componentsBridge.drawFrame()
  }
}
