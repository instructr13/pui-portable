package dev.wycey.mido.pui.components.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.rendering.MultiChildRendererElement

abstract class MultiChildRendererComponent(val children: List<Component> = listOf(), key: String? = null) :
  RendererComponent(key) {
  override fun createElement() = MultiChildRendererElement(this)
}
