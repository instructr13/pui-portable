package dev.wycey.mido.pui.elements.rendering

import dev.wycey.mido.pui.components.rendering.RendererComponent
import dev.wycey.mido.pui.renderer.RendererObject

public class EmptyRendererElement<T : RendererObject>(
  component: RendererComponent
) : RendererElement<T>(component) {
  override fun insertRendererChild(
    child: T,
    slot: Any?
  ): Nothing = throw UnsupportedOperationException("EmptyRendererElement cannot have children")

  override fun moveRendererChild(
    child: T,
    oldSlot: Any?,
    newSlot: Any?
  ): Nothing = throw UnsupportedOperationException("EmptyRendererElement cannot have children")

  override fun removeRendererChild(
    child: T,
    slot: Any?
  ): Nothing = throw UnsupportedOperationException("EmptyRendererElement cannot have children")
}
