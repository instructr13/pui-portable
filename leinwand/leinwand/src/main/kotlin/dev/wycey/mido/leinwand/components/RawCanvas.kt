package dev.wycey.mido.leinwand.components

import dev.wycey.mido.leinwand.renderer.CanvasRenderer
import dev.wycey.mido.pui.components.rendering.RendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.rendering.EmptyRendererElement
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.BoxRenderer

internal class RawCanvas(private val size: Size, private val instanceId: Int) : RendererComponent() {
  override fun createElement() = EmptyRendererElement<BoxRenderer>(this)

  override fun createRenderer(context: BuildContext) = CanvasRenderer(size, instanceId)

  override fun updateRenderer(
    context: BuildContext,
    renderer: RendererObject
  ) {
    (renderer as CanvasRenderer).size = size
  }
}
