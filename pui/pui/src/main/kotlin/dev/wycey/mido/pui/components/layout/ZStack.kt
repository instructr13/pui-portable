package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.MultiChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.AlignmentDirectional
import dev.wycey.mido.pui.layout.AlignmentFactor
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.layout.ZStackFit
import dev.wycey.mido.pui.renderer.layout.ZStackRenderer

public class ZStack
  @JvmOverloads
  constructor(
    children: List<Component> = emptyList(),
    private val alignment: AlignmentFactor = AlignmentDirectional.topStart,
    private val fit: ZStackFit = ZStackFit.Loose,
    key: String? = null
  ) : MultiChildRendererComponent(children, key) {
    @JvmOverloads
    public constructor(
      childrenBuilder: () -> List<Component>,
      alignment: AlignmentFactor = AlignmentDirectional.topStart,
      fit: ZStackFit = ZStackFit.Loose,
      key: String? = null
    ) : this(childrenBuilder(), alignment, fit, key)

    override fun createRenderer(context: BuildContext): ZStackRenderer =
      ZStackRenderer(
        alignment,
        fit
      )

    override fun updateRenderer(
      context: BuildContext,
      renderer: RendererObject
    ) {
      (renderer as ZStackRenderer).apply {
        this.alignment = alignment
        this.fit = fit
      }
    }
  }
