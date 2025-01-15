package dev.wycey.mido.pui.util.processing

import processing.core.PApplet

public enum class ColorMode {
  RGB,
  HSB

  ;

  public fun apply(applet: PApplet) {
    applet.colorMode(get())
  }

  public fun get(): Int =
    when (this) {
      RGB -> processing.core.PConstants.RGB
      HSB -> processing.core.PConstants.HSB
    }
}
