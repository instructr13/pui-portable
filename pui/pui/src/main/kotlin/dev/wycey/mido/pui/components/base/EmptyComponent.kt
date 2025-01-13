package dev.wycey.mido.pui.components.base

import dev.wycey.mido.pui.elements.base.EmptyElement

class EmptyComponent : Component() {
  override fun createElement() = EmptyElement(this)
}
