package dev.wycey.mido.pui.components.rendering

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.rendering.RendererElement
import dev.wycey.mido.pui.renderer.RendererObject

abstract class RendererComponent(key: String? = null) : Component(key) {
  abstract override fun createElement(): RendererElement<*>

  abstract fun createRenderer(context: BuildContext): RendererObject

  open fun updateRenderer(
    context: BuildContext,
    renderer: RendererObject
  ) {}

  open fun onUnmount(renderer: RendererObject) {}
}
