package dev.wycey.mido.pui.renderer.view

import dev.wycey.mido.pui.bridges.FrameEventBridge
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.layout.constraints.Constraints
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.delegations.RendererWithChild
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

class ViewRenderer(child: BoxRenderer? = null) : RendererWithChild<BoxRenderer>() {
  init {
    super.child = child
    that = this
  }

  lateinit var _configuration: ViewConfiguration

  var configuration: ViewConfiguration
    get() = _configuration
    set(value) {
      if (this::_configuration.isInitialized && value == _configuration) return

      _configuration = value

      markNeedsLayout()
    }

  override var constraints: Constraints? = BoxConstraints(maxWidth = 0f, maxHeight = 0f)

  var size: Size = Size.ZERO
    private set

  fun prepareInitialFrame() {
    scheduleInitialLayout()

    FrameEventBridge.instance.onPersistentDraw {
      context!!.nodesNeedingPaint.add(this)
    }
  }

  override fun performResize() = throw IllegalStateException("Cannot resize View")

  override fun performLayout() {
    size = configuration.size

    child?.layout(BoxConstraints.tight(size))
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    val applet = currentScope.applet

    applet.background(configuration.color)

    child?.tryPaint(d, currentScope)
  }
}
