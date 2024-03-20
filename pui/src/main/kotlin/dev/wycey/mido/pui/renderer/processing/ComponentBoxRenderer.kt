package dev.wycey.mido.pui.renderer.processing

import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.BorderRadius
import dev.wycey.mido.pui.renderer.box.ProxyBoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import dev.wycey.mido.pui.util.processing.RenderMode
import dev.wycey.mido.pui.util.processing.StrokeCaps
import dev.wycey.mido.pui.util.processing.StrokeJoins

class ComponentBoxRenderer(
  var fill: Int = 0x00FFFFFF,
  var stroke: Int = 0x00FFFFFF,
  var strokeWeight: Float? = null,
  var strokeCap: StrokeCaps? = null,
  var strokeJoin: StrokeJoins? = null,
  var mode: RenderMode? = null,
  var borderRadius: BorderRadius = BorderRadius.ZERO,
  var additionalPaint: ((d: AppletDrawer, currentScope: Scope, size: Size) -> Unit)? = null
) : ProxyBoxRenderer() {
  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    if (size > Size.ZERO) {
      d.with(
        fill = fill,
        stroke = stroke,
        strokeWeight = strokeWeight,
        strokeCap = strokeCap,
        strokeJoin = strokeJoin,
        rectMode = mode
      ) {
        d.rect(size, borderRadius)
      }

      additionalPaint?.invoke(d, currentScope, size)
    }

    child?.tryPaint(d, currentScope)
  }
}
