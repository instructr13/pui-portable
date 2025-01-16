package dev.wycey.mido.leinwand.renderer

import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import processing.core.PGraphics

internal class CanvasRenderer(
  initialSize: Size,
  private val instanceId: Int
) : BoxRenderer() {
  init {
    size = initialSize
  }

  private val handle get() = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!

  private var baseLayer: PGraphics = handle.applet.createGraphics(size.width.toInt(), size.height.toInt())

  override fun computeDryLayout(constraints: BoxConstraints) = constraints.constrain(size)

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    d.with(fill = 0xffffffff.toInt()) {
      d.rect(Point.ZERO, size)
    }

    baseLayer.beginDraw()
    baseLayer.background(255, 0f)

    for (layer in handle.layers) {
      layer.draw(baseLayer)
    }

    baseLayer.endDraw()

    d.image(baseLayer)

    handle.currentBaseLayer = baseLayer
  }
}
