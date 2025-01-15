package dev.wycey.mido.pui.components.text

import dev.wycey.mido.pui.util.processing.TextAlign

public data class TextStyle
  @JvmOverloads
  constructor(
    val color: Int = 0,
    val backgroundColor: Int = 0x00FFFFFF,
    val fontSize: Float = 20f,
    val textAlign: TextAlign = TextAlign.Left
  )
