package dev.wycey.mido.pui.elements.root

import dev.wycey.mido.pui.components.ComponentOwner
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.root.RootComponent
import dev.wycey.mido.pui.elements.base.Element

public class RootElement(
  component: RootComponent
) : Element(component) {
  private var child: Element? = null
  internal var newComponent: Component? = null

  override fun visitChildren(visitor: (element: Element) -> Unit) {
    if (child != null) visitor(child!!)
  }

  override fun mount(
    parent: Element?,
    newSlot: Any?
  ) {
    super.mount(parent, newSlot)

    rebuild()

    super.performRebuild()
  }

  override fun update(newComponent: Component) {
    super.update(newComponent)

    rebuild()
  }

  override fun performRebuild() {
    if (newComponent != null) {
      val newNewComponent = newComponent!!

      newComponent = null

      update(newNewComponent)
    }

    super.performRebuild()
  }

  private fun rebuild() {
    child = updateChild(child, (component as RootComponent).child, null)
  }

  internal fun assignOwner(owner: ComponentOwner) {
    this.owner = owner
  }
}
