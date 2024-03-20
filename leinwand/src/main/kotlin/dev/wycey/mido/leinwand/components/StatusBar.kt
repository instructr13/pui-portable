package dev.wycey.mido.leinwand.components

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.components.text.TextStyle
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets

class StatusBar(private val instanceId: Int) : StatefulComponent("statusBar$instanceId") {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!

    return Box(
      fill = 0xff2b2d30.toInt(),
      child =
        Padding(
          EdgeInsets.symmetric(1f, 4f),
          VStack(
            listOf(
              Text(
                handle.statusText,
                TextStyle(
                  color = 0xff8f8a8e.toInt(),
                  fontSize = 14f
                )
              )
            )
          )
        )
    )
  }
}
