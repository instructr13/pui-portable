package dev.wycey.mido.pui.components.base

import dev.wycey.mido.pui.elements.base.Element

abstract class Component(val key: String? = null) {
  companion object {
    @JvmStatic
    fun canUpdate(
      oldComponent: Component,
      newComponent: Component
    ): Boolean {
      return oldComponent::class == newComponent::class &&
        oldComponent.key == newComponent.key
    }
  }

  abstract fun createElement(): Element
}
