package dev.wycey.mido.leinwand.draw

import processing.core.PGraphics

internal interface Drawable {
  fun draw(base: PGraphics)
}
