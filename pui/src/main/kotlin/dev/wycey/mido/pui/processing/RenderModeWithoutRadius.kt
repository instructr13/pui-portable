package dev.wycey.mido.pui.util.processing

enum class RenderModeWithoutRadius {
  Corner,
  Corners,
  Center

  ;

  fun apply(applet: processing.core.PApplet) {
    applet.rectMode(get())
  }

  fun get() =
    when (this) {
      Corner -> processing.core.PConstants.CORNER
      Corners -> processing.core.PConstants.CORNERS
      Center -> processing.core.PConstants.CENTER
    }
}
