package dev.wycey.mido.leinwand.components

import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.input.Button
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.layout.EdgeInsets

internal class BrushSelector(
  private val instanceId: Int
) : StatefulComponent("brushSelector$instanceId") {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!

    effect { handle.currentToolIndex }

    return Padding(
      EdgeInsets.symmetric(4f, 8f),
      HStack(
        handle.tools.withIndex().map { (i, tool) ->
          Button(
            Text(tool.name, style = Styles.title),
            Styles.topBarButton.let {
              if (i == handle.currentToolIndex) it.copy(normalColor = 0xff34363a.toInt()) else it
            },
            onClick = onClick@{ _, type ->
              if (type.button != MouseButtons.LEFT) return@onClick

              handle.currentToolIndex = i
            },
            key = "toolButton${tool.name}${i == handle.currentToolIndex}"
          )
        }
      )
    )
  }
}
