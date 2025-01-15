package dev.wycey.mido.pui.components.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.rendering.SingleChildRendererElement

public abstract class SingleChildRendererComponent(key: String?, public val child: Component? = null) :
  RendererComponent(key) {
  override fun createElement(): SingleChildRendererElement = SingleChildRendererElement(this)
}
