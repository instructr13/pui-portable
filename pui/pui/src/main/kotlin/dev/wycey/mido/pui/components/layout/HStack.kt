package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.MultiChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment
import dev.wycey.mido.pui.renderer.layout.StackDirection
import dev.wycey.mido.pui.renderer.layout.StackMainAxisAlignment
import dev.wycey.mido.pui.renderer.layout.StackRenderer

internal typealias ChildrenBuilder = () -> List<Component>

public class HStack
  @JvmOverloads
  constructor(
    children: List<Component>,
    private val mainAxisAlignment: StackMainAxisAlignment = StackMainAxisAlignment.Start,
    private val crossAxisAlignment: StackCrossAxisAlignment = StackCrossAxisAlignment.Start,
    key: String? = null
  ) : MultiChildRendererComponent(children, key) {
    @JvmOverloads
    public constructor(
      builder: ChildrenBuilder,
      mainAxisAlignment: StackMainAxisAlignment = StackMainAxisAlignment.Start,
      crossAxisAlignment: StackCrossAxisAlignment = StackCrossAxisAlignment.Start,
      key: String? = null
    ) : this(builder(), mainAxisAlignment, crossAxisAlignment, key)

    override fun createRenderer(context: BuildContext): StackRenderer =
      StackRenderer(
        StackDirection.Horizontal,
        mainAxisAlignment,
        crossAxisAlignment
      )
  }
