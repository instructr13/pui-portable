package dev.wycey.mido.pui.elements.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.ParentRendererDataComponent
import dev.wycey.mido.pui.components.rendering.RendererComponent
import dev.wycey.mido.pui.elements.base.Element
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.data.ParentRendererData

public abstract class RendererElement<T : RendererObject>(component: RendererComponent) : Element(component) {
  private var _renderer: T? = null
  override val renderer: RendererObject
    get() = _renderer!!

  override val rendererAttachingChild: Element? = null

  private var ancestorRendererElement: RendererElement<RendererObject>? = null

  private fun findAncestorRendererElement(): RendererElement<RendererObject> {
    var ancestor = parent

    while (ancestor != null && ancestor !is RendererElement<*>) {
      ancestor = ancestor.parent
    }

    return ancestor as RendererElement<RendererObject>
  }

  private fun findAncestorParentRendererDataElements(): List<ParentRendererDataElement<ParentRendererData>> {
    var ancestor = parent
    val result = mutableListOf<ParentRendererDataElement<ParentRendererData>>()

    while (ancestor != null && ancestor !is RendererElement<*>) {
      if (ancestor is ParentRendererDataElement<*>) {
        result.add(ancestor as ParentRendererDataElement<ParentRendererData>)
      }

      ancestor = ancestor.parent
    }

    return result
  }

  private fun _performRebuild() {
    (component as RendererComponent).updateRenderer(this, renderer)

    super.performRebuild()
  }

  override fun performRebuild() {
    _performRebuild()
  }

  override fun mount(
    parent: Element?,
    newSlot: Any?
  ) {
    super.mount(parent, newSlot)

    _renderer = (component as RendererComponent).createRenderer(this) as T
    attachRenderer(newSlot)

    rebuild()
  }

  override fun update(newComponent: Component) {
    super.update(newComponent)

    _performRebuild()
  }

  override fun unmount() {
    val oldComponent = component as RendererComponent

    super.unmount()

    oldComponent.onUnmount(renderer)

    _renderer = null
  }

  override fun updateSlot(newSlot: Any?) {
    val oldSlot = slot

    super.updateSlot(newSlot)

    ancestorRendererElement?.moveRendererChild(renderer, oldSlot, slot)
  }

  override fun attachRenderer(newSlot: Any?) {
    slot = newSlot
    ancestorRendererElement = findAncestorRendererElement()

    ancestorRendererElement?.insertRendererChild(renderer, slot)

    val parentRendererDataElements = findAncestorParentRendererDataElements()

    for (parentRendererDataElement in parentRendererDataElements) {
      updateParentRendererData(parentRendererDataElement.component as ParentRendererDataComponent<ParentRendererData>)
    }
  }

  override fun detachRenderer() {
    ancestorRendererElement?.removeRendererChild(renderer, slot)
    ancestorRendererElement = null

    slot = null
  }

  internal fun <T : ParentRendererData> updateParentRendererData(
    parentRendererDataComponent: ParentRendererDataComponent<T>
  ) {
    parentRendererDataComponent.applyParentRendererData(renderer)
  }

  protected abstract fun insertRendererChild(
    child: T,
    slot: Any?
  )

  protected abstract fun moveRendererChild(
    child: T,
    oldSlot: Any?,
    newSlot: Any?
  )

  protected abstract fun removeRendererChild(
    child: T,
    slot: Any?
  )
}
