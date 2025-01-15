package dev.wycey.mido.pui.components.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.rendering.RendererElement
import dev.wycey.mido.pui.renderer.RendererObject

public abstract class RendererComponent(key: String? = null) : Component(key) {
  public abstract override fun createElement(): RendererElement<*>

  public abstract fun createRenderer(context: BuildContext): RendererObject

  public open fun updateRenderer(
    context: BuildContext,
    renderer: RendererObject
  ) {
  }

  public open fun onUnmount(renderer: RendererObject) {}
}
