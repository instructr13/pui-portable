package dev.wycey.mido.pui.components.input

import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.renderer.BorderRadius

data class ButtonStyle
  @JvmOverloads
  constructor(
    val disabled: Boolean = false,
    val childPadding: EdgeInsets = EdgeInsets.symmetric(10f, 20f),
    val borderRadius: BorderRadius = BorderRadius.all(12f),
    val borderColor: Int = 0xf0bfbfbf.toInt(),
    val normalColor: Int = 0xffdddddd.toInt(),
    val hoverColor: Int = 0xffeeeeee.toInt(),
    val pressedColor: Int = 0xffcccccc.toInt(),
    val disabledColor: Int = 0xffaaaaaa.toInt()
  )
