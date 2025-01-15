package dev.wycey.mido.pui.components.base

import dev.wycey.mido.pui.elements.base.EmptyElement

public class EmptyComponent : Component() {
  override fun createElement(): EmptyElement = EmptyElement(this)
}
