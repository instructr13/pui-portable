package dev.wycey.mido.pui.elements.rendering

import dev.wycey.mido.pui.components.rendering.RendererComponent
import dev.wycey.mido.pui.renderer.RendererObject

class EmptyRendererElement<T : RendererObject>(component: RendererComponent) : RendererElement<T>(component) {
  override fun insertRendererChild(
    child: T,
    slot: Any?
  ) = throw UnsupportedOperationException("EmptyRendererElement cannot have children")

  override fun moveRendererChild(
    child: T,
    oldSlot: Any?,
    newSlot: Any?
  ) = throw UnsupportedOperationException("EmptyRendererElement cannot have children")

  override fun removeRendererChild(
    child: T,
    slot: Any?
  ) = throw UnsupportedOperationException("EmptyRendererElement cannot have children")
}
