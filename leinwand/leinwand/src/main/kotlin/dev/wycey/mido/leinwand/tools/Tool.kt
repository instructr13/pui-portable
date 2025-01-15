package dev.wycey.mido.leinwand.tools

import processing.core.PApplet

internal interface Tool {
  val name: String

  fun applyCursor(applet: PApplet)
}
