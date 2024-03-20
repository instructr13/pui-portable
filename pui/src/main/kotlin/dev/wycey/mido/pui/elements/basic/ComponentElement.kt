package dev.wycey.mido.pui.elements.basic

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.ParentRendererDataComponent
import dev.wycey.mido.pui.elements.base.Element
import dev.wycey.mido.pui.renderer.data.ParentRendererData

abstract class ComponentElement(component: Component) : Element(component) {
  var child: Element? = null
    private set

  override fun mount(
    parent: Element?,
    newSlot: Any?
  ) {
    super.mount(parent, newSlot)
    firstBuild()
  }

  protected open fun firstBuild() {
    rebuild()
  }

  override fun performRebuild() {
    val built = build()

    super.performRebuild()

    try {
      child = updateChild(child, built, slot)
    } catch (e: Exception) {
      e.printStackTrace()

      child = updateChild(null, built, slot)
    }
  }

  override fun visitChildren(visitor: (element: Element) -> Unit) {
    if (child != null) visitor(child!!)
  }

  fun _updateParentData(parentRendererDataComponent: ParentRendererDataComponent<ParentRendererData>) {
    parentRendererDataComponent.applyParentRendererData(renderer!!)
  }

  protected abstract fun build(): Component
}
