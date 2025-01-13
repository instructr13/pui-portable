package dev.wycey.mido.pui.components.processing

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.processing.EllipseRenderer
import dev.wycey.mido.pui.util.processing.RenderMode
import dev.wycey.mido.pui.util.processing.StrokeCaps
import dev.wycey.mido.pui.util.processing.StrokeJoins

class Ellipse
  @JvmOverloads
  constructor(
    child: Component? = null,
    val fill: Int = 0x00FFFFFF,
    val stroke: Int = 0x00FFFFFF,
    val strokeWeight: Float? = null,
    val strokeCap: StrokeCaps? = null,
    val strokeJoin: StrokeJoins? = null,
    val mode: RenderMode = RenderMode.Corner,
    key: String? = null
  ) :
  SingleChildRendererComponent(key, child) {
    override fun createRenderer(context: BuildContext) =
      EllipseRenderer(
        fill = fill,
        stroke = stroke,
        strokeWeight = strokeWeight,
        strokeCap = strokeCap,
        strokeJoin = strokeJoin,
        mode = mode
      )

    override fun updateRenderer(
      context: BuildContext,
      renderer: RendererObject
    ) {
      super.updateRenderer(context, renderer)

      if (renderer is EllipseRenderer) {
        renderer.fill = fill
        renderer.stroke = stroke
        renderer.strokeWeight = strokeWeight
        renderer.strokeCap = strokeCap
        renderer.strokeJoin = strokeJoin
        renderer.mode = mode
      }
    }
  }
