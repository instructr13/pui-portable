package dev.wycey.mido.pui.examples.colors

import dev.wycey.mido.pui.components.rendering.RendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.rendering.EmptyRendererElement
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.BoxRenderer

internal class HSLuvRenderHueBar(
  private val h: Float,
  private val s: Float,
  private val l: Float,
  key: String? = null
) : RendererComponent(key) {
  override fun createElement() = EmptyRendererElement<BoxRenderer>(this)

  override fun createRenderer(context: BuildContext) = HSLuvHueBarRenderer(h, s, l)

  override fun updateRenderer(
    context: BuildContext,
    renderer: RendererObject
  ) {
    (renderer as HSLuvHueBarRenderer).h = h
    renderer.s = s
    renderer.l = l
  }
}
