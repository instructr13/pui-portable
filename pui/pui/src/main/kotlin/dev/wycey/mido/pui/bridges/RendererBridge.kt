package dev.wycey.mido.pui.bridges

import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.RenderGlobalContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.view.ViewConfiguration
import dev.wycey.mido.pui.renderer.view.ViewRenderer

interface RendererBridgeContract {
  fun drawFrame()
}

class RendererBridge internal constructor(
  private val mouseEventBridge: MouseEventBridge,
  private val frameEventBridge: FrameEventBridge,
  private val displayBridge: DisplayBridge
) : RendererBridgeContract,
  MouseEventBridgeContract by mouseEventBridge,
  FrameEventBridgeContract by frameEventBridge,
  DisplayBridgeContract by displayBridge,
  BridgeBase() {
  companion object {
    @JvmField
    var instanceNullable: RendererBridge? = null

    @JvmStatic
    val instance get() = checkInstance(instanceNullable)

    @JvmStatic
    lateinit var rootContext: RenderGlobalContext
      private set
  }

  private val view = mutableListOf<ViewRenderer>()

  private fun createRootContext(): RenderGlobalContext =
    object : RenderGlobalContext(rootScope) {
      override var rootNode: RendererObject? = null
        set(_) = throw IllegalStateException("Cannot set root node")
    }

  override fun initInstance() {
    displayBridge.initInstance()
    instanceNullable = this

    rootContext = createRootContext()

    println("Renderer bridge initialized")
  }

  override fun drawFrame() {
    rootContext.flushLayout()
    rootContext.flushPaint()
  }

  fun addRenderView(view: ViewRenderer) {
    view.configuration = createViewConfiguration()

    applet.background(view.configuration.color)

    this.view.add(view)
  }

  private fun createViewConfiguration(): ViewConfiguration =
    ViewConfiguration(
      255,
      Size(applet.width.toFloat(), applet.height.toFloat())
    )

  fun removeRenderView(view: ViewRenderer) {
    this.view.remove(view)
  }
}
