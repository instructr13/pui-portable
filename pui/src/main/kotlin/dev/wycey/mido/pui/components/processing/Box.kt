package dev.wycey.mido.pui.components.processing

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.BorderRadius
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.processing.ComponentBoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import dev.wycey.mido.pui.util.processing.RenderMode
import dev.wycey.mido.pui.util.processing.StrokeCaps
import dev.wycey.mido.pui.util.processing.StrokeJoins

class Box
  @JvmOverloads
  constructor(
    child: Component? = null,
    val fill: Int = 0x00FFFFFF,
    val stroke: Int = 0x00FFFFFF,
    val strokeWeight: Float? = null,
    val strokeCap: StrokeCaps? = null,
    val strokeJoin: StrokeJoins? = null,
    val borderRadius: BorderRadius = BorderRadius.ZERO,
    val mode: RenderMode? = null,
    key: String? = null,
    val additionalPaint: ((d: AppletDrawer, currentScope: Scope, size: Size) -> Unit)? = null
  ) :
  SingleChildRendererComponent(key, child) {
    override fun createRenderer(context: BuildContext) =
      ComponentBoxRenderer(
        fill = fill,
        stroke = stroke,
        strokeWeight = strokeWeight,
        strokeCap = strokeCap,
        strokeJoin = strokeJoin,
        borderRadius = borderRadius,
        mode = mode,
        additionalPaint = additionalPaint
      )

    override fun updateRenderer(
      context: BuildContext,
      renderer: RendererObject
    ) {
      super.updateRenderer(context, renderer)

      if (renderer is ComponentBoxRenderer) {
        renderer.fill = fill
        renderer.stroke = stroke
        renderer.strokeWeight = strokeWeight
        renderer.strokeCap = strokeCap
        renderer.strokeJoin = strokeJoin
        renderer.borderRadius = borderRadius
        renderer.mode = mode
        renderer.additionalPaint = additionalPaint
      }
    }
  }
