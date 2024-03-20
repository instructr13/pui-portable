package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.MultiChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment
import dev.wycey.mido.pui.renderer.layout.StackMainAxisAlignment

typealias ChildrenBuilder = () -> List<Component>

class HStack
  @JvmOverloads
  constructor(
    children: List<Component>,
    private val mainAxisAlignment: StackMainAxisAlignment = StackMainAxisAlignment.Start,
    private val crossAxisAlignment: StackCrossAxisAlignment = StackCrossAxisAlignment.Start,
    key: String? = null
  ) : MultiChildRendererComponent(children, key) {
    @JvmOverloads
    constructor(
      builder: ChildrenBuilder,
      mainAxisAlignment: StackMainAxisAlignment = StackMainAxisAlignment.Start,
      crossAxisAlignment: StackCrossAxisAlignment = StackCrossAxisAlignment.Start,
      key: String? = null
    ) : this(builder(), mainAxisAlignment, crossAxisAlignment, key)

    override fun createRenderer(context: BuildContext) =
      dev.wycey.mido.pui.renderer.layout.StackRenderer(
        dev.wycey.mido.pui.renderer.layout.StackDirection.Horizontal,
        mainAxisAlignment,
        crossAxisAlignment
      )
  }
