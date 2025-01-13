package dev.wycey.mido.pui.components.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.ProxyComponent
import dev.wycey.mido.pui.elements.rendering.ParentRendererDataElement
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.data.ParentRendererData

abstract class ParentRendererDataComponent<T : ParentRendererData>(child: Component, key: String? = null) :
  ProxyComponent(child, key) {
  override fun createElement() = ParentRendererDataElement(this)

  abstract fun applyParentRendererData(renderer: RendererObject)
}
