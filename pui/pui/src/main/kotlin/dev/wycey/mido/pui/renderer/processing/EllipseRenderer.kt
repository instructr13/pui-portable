package dev.wycey.mido.pui.renderer.processing

import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.box.ProxyBoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import dev.wycey.mido.pui.util.processing.RenderMode
import dev.wycey.mido.pui.util.processing.StrokeCaps
import dev.wycey.mido.pui.util.processing.StrokeJoins

public class EllipseRenderer(
  public var fill: Int = 0x00FFFFFF,
  public var stroke: Int = 0x00FFFFFF,
  public var strokeWeight: Float? = null,
  public var strokeCap: StrokeCaps? = null,
  public var strokeJoin: StrokeJoins? = null,
  public var mode: RenderMode? = null
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
        ellipseMode = mode
      ) {
        d.ellipse(size)
      }
    }

    child?.tryPaint(d, currentScope)
  }
}
