package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.layout.PaddingRenderer

public class Padding
  @JvmOverloads
  constructor(
    private val padding: EdgeInsets,
    child: Component? = null,
    key: String? = null
  ) :
  SingleChildRendererComponent(key, child) {
    override fun createRenderer(context: BuildContext): PaddingRenderer = PaddingRenderer(padding)

    override fun updateRenderer(
      context: BuildContext,
      renderer: RendererObject
    ) {
      (renderer as PaddingRenderer).padding = padding
    }
  }
