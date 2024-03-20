package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.MultiChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment
import dev.wycey.mido.pui.renderer.layout.StackMainAxisAlignment

class VStack
  @JvmOverloads
  constructor(
    children: List<Component>,
    key: String? = null,
    private val mainAxisAlignment: StackMainAxisAlignment = StackMainAxisAlignment.Start,
    private val crossAxisAlignment: StackCrossAxisAlignment = StackCrossAxisAlignment.Start
  ) : MultiChildRendererComponent(children, key) {
    constructor(
      builder: ChildrenBuilder,
      key: String? = null,
      mainAxisAlignment: StackMainAxisAlignment = StackMainAxisAlignment.Start,
      crossAxisAlignment: StackCrossAxisAlignment = StackCrossAxisAlignment.Start
    ) : this(builder(), key, mainAxisAlignment, crossAxisAlignment)

    override fun createRenderer(context: BuildContext) =
      dev.wycey.mido.pui.renderer.layout.StackRenderer(
        dev.wycey.mido.pui.renderer.layout.StackDirection.Vertical,
        mainAxisAlignment,
        crossAxisAlignment
      )
  }
