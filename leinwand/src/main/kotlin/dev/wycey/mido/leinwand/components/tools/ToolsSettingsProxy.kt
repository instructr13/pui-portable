package dev.wycey.mido.leinwand.components.tools

import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.leinwand.tools.brush.Brush
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets

class ToolsSettingsProxy(private val instanceId: Int) : StatefulComponent("toolsSettingsProxy$instanceId") {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!
    val tool = handle.currentTool

    return VStack(
      {
        val list =
          mutableListOf<Component>(
            VirtualBox(
              height = 22f,
              child =
                HStack(
                  listOf(
                    VirtualBox(
                      width = 8f,
                      child =
                        Box(
                          fill = 0xff3574f0.toInt()
                        )
                    ),
                    VirtualBox(width = 8f),
                    Padding(
                      EdgeInsets.only(top = 4f),
                      Text(tool.name, Styles.text.copy(fontSize = 20f))
                    )
                  ),
                  crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Stretch
                )
            ),
            VirtualBox(height = 8f)
          )

        if (tool is Brush) {
          list.add(
            BrushSettings(tool, "brushSettings$instanceId")
          )
        }

        list
      },
      crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Stretch,
      key = "toolSettings${tool.name}"
    )
  }
}
