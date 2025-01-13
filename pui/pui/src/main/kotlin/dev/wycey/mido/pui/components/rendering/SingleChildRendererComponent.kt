package dev.wycey.mido.pui.components.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.rendering.SingleChildRendererElement

abstract class SingleChildRendererComponent(key: String?, val child: Component? = null) : RendererComponent(key) {
  override fun createElement() = SingleChildRendererElement(this)
}
