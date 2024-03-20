package dev.wycey.mido.pui.elements.basic

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.ProxyComponent

abstract class ProxyElement(component: ProxyComponent) : ComponentElement(component) {
  override fun build() = (component as ProxyComponent).child

  override fun update(newComponent: Component) {
    val oldComponent = component as ProxyComponent

    super.update(newComponent)

    updated(oldComponent)

    rebuild(true)
  }

  protected fun updated(oldComponent: ProxyComponent) {
    notifyUpdate(oldComponent.child)
  }

  protected abstract fun notifyUpdate(oldComponent: Component)
}
