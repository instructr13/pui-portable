package dev.wycey.mido.pui.elements.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.ParentRendererDataComponent
import dev.wycey.mido.pui.elements.base.Element
import dev.wycey.mido.pui.elements.basic.ProxyElement
import dev.wycey.mido.pui.renderer.data.ParentRendererData

public class ParentRendererDataElement<T : ParentRendererData>(component: ParentRendererDataComponent<T>) :
  ProxyElement(component) {
  private fun applyParentRendererData(component: ParentRendererDataComponent<T>) {
    fun applyParentRendererDataToChild(child: Element) {
      if (child is RendererElement<*>) {
        child.updateParentRendererData(component)
      } else if (child.rendererAttachingChild != null) {
        applyParentRendererDataToChild(child.rendererAttachingChild!!)
      }
    }

    if (rendererAttachingChild != null) {
      applyParentRendererDataToChild(rendererAttachingChild!!)
    }
  }

  override fun notifyUpdate(oldComponent: Component) {
    applyParentRendererData(component as ParentRendererDataComponent<T>)
  }
}
