package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.ParentRendererDataComponent
import dev.wycey.mido.pui.renderer.RendererObject

public open class Flexible
  @JvmOverloads
  constructor(
    child: Component,
    private val flex: Int = 1,
    private val fit: dev.wycey.mido.pui.renderer.layout.StackFit = dev.wycey.mido.pui.renderer.layout.StackFit.Loose,
    key: String? = null
  ) : ParentRendererDataComponent<dev.wycey.mido.pui.renderer.layout.StackParentRendererData>(child, key) {
    override fun applyParentRendererData(renderer: RendererObject) {
      val data = renderer.parentRendererData!! as dev.wycey.mido.pui.renderer.layout.StackParentRendererData
      var needsLayout = false

      if (data.flex != flex) {
        data.flex = flex

        needsLayout = true
      }

      if (data.fit != fit) {
        data.fit = fit

        needsLayout = true
      }

      if (needsLayout) {
        renderer.parent?.markNeedsLayout()
      }
    }
  }
