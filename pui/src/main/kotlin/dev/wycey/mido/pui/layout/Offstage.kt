package dev.wycey.mido.pui.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.layout.OffStageRenderer

class Offstage(
  child: Component? = null,
  val offstage: Boolean = true,
  key: String? = null
) :
  SingleChildRendererComponent(key, child) {
  override fun createRenderer(context: BuildContext) = OffStageRenderer(offstage)

  override fun updateRenderer(
    context: BuildContext,
    renderer: RendererObject
  ) {
    (renderer as OffStageRenderer).offstage = offstage
  }
}
