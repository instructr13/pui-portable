package dev.wycey.mido.pui.components.root

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.rendering.RendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.root.ViewElement
import dev.wycey.mido.pui.renderer.view.ViewRenderer

internal class ViewComponent(val child: Component, key: String? = null) :
  StatelessComponent(key) {
  inner class InnerViewComponent(private val _builder: () -> Component) :
    RendererComponent() {
    override fun createElement() = ViewElement(this)

    override fun createRenderer(context: BuildContext) = ViewRenderer()

    fun builder() = _builder()
  }

  override fun build(context: BuildContext): Component =
    InnerViewComponent {
      child
    }
}
