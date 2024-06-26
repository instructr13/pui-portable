package dev.wycey.mido.pui.elements.basic

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatelessComponent

class StatelessElement(component: StatelessComponent) : ComponentElement(component) {
  override fun build() = (component as StatelessComponent).build(this)

  override fun update(newComponent: Component) {
    super.update(newComponent)

    rebuild(true)
  }
}
