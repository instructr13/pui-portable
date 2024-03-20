package dev.wycey.mido.pui.components.text

import dev.wycey.mido.pui.components.rendering.RendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.rendering.EmptyRendererElement
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.text.TextRenderer

internal class RawText(private val content: String, private val textStyle: TextStyle, key: String? = null) :
  RendererComponent(key) {
  override fun createElement() = EmptyRendererElement<BoxRenderer>(this)

  override fun createRenderer(context: BuildContext) = TextRenderer(content, textStyle)

  override fun updateRenderer(
    context: BuildContext,
    renderer: RendererObject
  ) {
    (renderer as TextRenderer).content = content
    renderer.textStyle = textStyle
  }
}
