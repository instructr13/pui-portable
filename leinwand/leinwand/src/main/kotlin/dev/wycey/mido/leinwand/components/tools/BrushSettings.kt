package dev.wycey.mido.leinwand.components.tools

import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.leinwand.tools.brush.Brush
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext

internal class BrushSettings(
  private val brush: Brush,
  key: String? = null
) : StatelessComponent(key) {
  override fun build(context: BuildContext) =
    VStack(
      listOf(
        Text("Size", Styles.text),
        VirtualBox(
          height = 80f,
          child = BrushSizeSettings(brush)
        )
      )
    )
}
