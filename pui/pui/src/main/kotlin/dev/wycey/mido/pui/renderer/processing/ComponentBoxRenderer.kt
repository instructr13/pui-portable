package dev.wycey.mido.pui.renderer.processing

import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.BorderRadius
import dev.wycey.mido.pui.renderer.box.ProxyBoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import dev.wycey.mido.pui.util.processing.RenderMode
import dev.wycey.mido.pui.util.processing.StrokeCaps
import dev.wycey.mido.pui.util.processing.StrokeJoins

public class ComponentBoxRenderer(
  public var fill: Int = 0x00FFFFFF,
  public var stroke: Int = 0x00FFFFFF,
  public var strokeWeight: Float? = null,
  public var strokeCap: StrokeCaps? = null,
  public var strokeJoin: StrokeJoins? = null,
  public var mode: RenderMode? = null,
  public var borderRadius: BorderRadius = BorderRadius.ZERO,
  public var additionalPaint: ((d: AppletDrawer, currentScope: Scope, size: Size) -> Unit)? = null
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
