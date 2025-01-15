package dev.wycey.mido.leinwand

import dev.wycey.mido.pui.components.input.ButtonStyle
import dev.wycey.mido.pui.components.text.TextStyle
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.renderer.BorderRadius

internal object Styles {
  val title =
    TextStyle(
      color = 0xffc3cdd1.toInt(),
      fontSize = 20f
    )

  val text =
    TextStyle(
      color = 0xffc3cdd1.toInt(),
      fontSize = 16f
    )

  val topBarButton =
    ButtonStyle(
      childPadding = EdgeInsets.symmetric(6f, 10f),
      borderRadius = BorderRadius.all(4f),
      borderColor = 0x00ffffff,
      normalColor = 0x00ffffff,
      hoverColor = 0x26000000,
      pressedColor = 0x38000000
    )

  val sidebarButton =
    ButtonStyle(
      childPadding = EdgeInsets.symmetric(4f, 8f),
      borderRadius = BorderRadius.all(6f),
      borderColor = 0xff4b4d50.toInt(),
      normalColor = 0xff2b2d30.toInt(),
      hoverColor = 0xff3b3d40.toInt(),
      pressedColor = 0xff1b1d20.toInt(),
      disabledColor = 0xff1b1d20.toInt()
    )
}
