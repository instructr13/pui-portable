package dev.wycey.mido.pui.elements.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.IndexedSlot
import dev.wycey.mido.pui.components.rendering.MultiChildRendererComponent
import dev.wycey.mido.pui.elements.base.Element
import dev.wycey.mido.pui.elements.base.NullElement
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererContract

open class MultiChildRendererElement(component: MultiChildRendererComponent) :
  RendererElement<RendererObject>(component) {
  fun getRenderer() = renderer as ContainerRendererContract<RendererObject>

  protected val children get() = _children.toList()

  private var _children = mutableListOf<Element>()

  override fun insertRendererChild(
    child: RendererObject,
    slot: Any?
  ) {
    val renderer = getRenderer()
    val indexedSlot = slot as IndexedSlot<Element?>

    renderer.insert(child, after = indexedSlot.value?.renderer)
  }

  override fun moveRendererChild(
    child: RendererObject,
    oldSlot: Any?,
    newSlot: Any?
  ) {
    val renderer = getRenderer()
    val newIndexedSlot = newSlot as IndexedSlot<Element?>

    renderer.move(child, after = newIndexedSlot.value?.renderer)
  }

  override fun removeRendererChild(
    child: RendererObject,
    slot: Any?
  ) {
    val renderer = getRenderer()

    renderer.remove(child)
  }

  override fun mount(
    parent: Element?,
    newSlot: Any?
  ) {
    super.mount(parent, newSlot)

    val multiChildRendererComponent = component as MultiChildRendererComponent

    val children: MutableList<Element> =
      arrayOfNulls<Element>(multiChildRendererComponent.children.size).map { NullElement }
        .toMutableList()

    var previousChild: Element? = null

    for ((i, _) in children.withIndex()) {
      val newChild = inflateComponent(multiChildRendererComponent.children[i], IndexedSlot(i, previousChild))

      children[i] = newChild

      previousChild = newChild
    }

    _children = children
  }

  override fun update(newComponent: Component) {
    super.update(newComponent)

    val multiChildRendererComponent = component as MultiChildRendererComponent

    _children = updateChildren(_children, multiChildRendererComponent.children)
  }
}
