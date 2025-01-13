package dev.wycey.mido.pui.elements.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.Element
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.delegations.RendererWithChildContract

class SingleChildRendererElement(component: SingleChildRendererComponent) : RendererElement<RendererObject>(component) {
  private var child: Element? = null

  override fun visitChildren(visitor: (element: Element) -> Unit) {
    if (child != null) {
      visitor(child!!)
    }
  }

  override fun mount(
    parent: Element?,
    newSlot: Any?
  ) {
    super.mount(parent, newSlot)

    child = updateChild(child, (component as SingleChildRendererComponent).child, null)
  }

  override fun update(newComponent: Component) {
    super.update(newComponent)

    child = updateChild(child, (component as SingleChildRendererComponent).child, null)
  }

  override fun insertRendererChild(
    child: RendererObject,
    slot: Any?
  ) {
    val renderer = this.renderer as RendererWithChildContract<RendererObject>

    renderer.child = child
  }

  override fun moveRendererChild(
    child: RendererObject,
    oldSlot: Any?,
    newSlot: Any?
  ) {
    throw Exception("Cannot move child of SingleChildRendererComponent")
  }

  override fun removeRendererChild(
    child: RendererObject,
    slot: Any?
  ) {
    val renderer = this.renderer as RendererWithChildContract<RendererObject>

    renderer.child = null
  }
}
