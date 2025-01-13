package dev.wycey.mido.pui.components.root

import dev.wycey.mido.pui.components.ComponentOwner
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.root.RootElement

class RootComponent(var child: Component? = null) : Component() {
  override fun createElement(): RootElement = RootElement(this)

  fun attach(
    owner: ComponentOwner,
    rootElement: RootElement?
  ): RootElement {
    if (rootElement != null) {
      rootElement.newComponent = this
      rootElement.markAsDirty()

      return rootElement
    }

    return createElement().apply {
      assignOwner(owner)

      owner.buildScope {
        mount(null, null)

        println("Root component attached")
      }
    }
  }
}
