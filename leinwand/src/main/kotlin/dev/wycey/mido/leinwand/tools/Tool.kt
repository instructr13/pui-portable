package dev.wycey.mido.leinwand.tools

import processing.core.PApplet

interface Tool {
  val name: String

  fun applyCursor(applet: PApplet)
}
