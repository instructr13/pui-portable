package dev.wycey.mido.pui.components.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.rendering.MultiChildRendererElement

public abstract class MultiChildRendererComponent(
  public val children: List<Component> = listOf(),
  key: String? = null
) : RendererComponent(key) {
  override fun createElement(): MultiChildRendererElement = MultiChildRendererElement(this)
}
