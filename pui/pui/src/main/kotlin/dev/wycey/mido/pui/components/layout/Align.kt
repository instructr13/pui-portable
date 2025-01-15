package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.Alignment
import dev.wycey.mido.pui.layout.AlignmentFactor
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.PositionedBoxRenderer

public open class Align
  @JvmOverloads
  constructor(
    private val alignment: AlignmentFactor = Alignment.center,
    child: Component? = null,
    key: String? = null
  ) :
  SingleChildRendererComponent(key, child) {
    override fun createRenderer(context: BuildContext): PositionedBoxRenderer = PositionedBoxRenderer(alignment)

    override fun updateRenderer(
      context: BuildContext,
      renderer: RendererObject
    ) {
      (renderer as PositionedBoxRenderer).alignment = alignment
    }
  }
