package dev.wycey.mido.pui.util.processing

enum class ColorMode {
  RGB,
  HSB

  ;

  fun apply(applet: processing.core.PApplet) {
    applet.colorMode(get())
  }

  fun get() =
    when (this) {
      RGB -> processing.core.PConstants.RGB
      HSB -> processing.core.PConstants.HSB
    }
}
