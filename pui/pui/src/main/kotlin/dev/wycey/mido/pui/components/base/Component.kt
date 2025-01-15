package dev.wycey.mido.pui.components.base

import dev.wycey.mido.pui.elements.base.Element

public abstract class Component(public val key: String? = null) {
  public companion object {
    @JvmStatic
    public fun canUpdate(
      oldComponent: Component,
      newComponent: Component
    ): Boolean {
      return oldComponent::class == newComponent::class &&
        oldComponent.key == newComponent.key
    }
  }

  internal abstract fun createElement(): Element
}
