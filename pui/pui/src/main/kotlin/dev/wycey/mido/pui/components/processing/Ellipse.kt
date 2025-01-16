package dev.wycey.mido.pui.components.processing

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.processing.EllipseRenderer
import dev.wycey.mido.pui.util.processing.RenderMode
import dev.wycey.mido.pui.util.processing.StrokeCaps
import dev.wycey.mido.pui.util.processing.StrokeJoins

public class Ellipse
  @JvmOverloads
  constructor(
    child: Component? = null,
    private val fill: Int = 0x00FFFFFF,
    private val stroke: Int = 0x00FFFFFF,
    private val strokeWeight: Float? = null,
    private val strokeCap: StrokeCaps? = null,
    private val strokeJoin: StrokeJoins? = null,
    private val mode: RenderMode = RenderMode.Corner,
    key: String? = null
  ) : SingleChildRendererComponent(key, child) {
    override fun createRenderer(context: BuildContext): EllipseRenderer =
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
