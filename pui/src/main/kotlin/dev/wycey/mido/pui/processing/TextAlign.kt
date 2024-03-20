package dev.wycey.mido.pui.util.processing

enum class TextAlign {
  Left,
  Center,
  Right

  ;

  fun apply(applet: processing.core.PApplet) {
    applet.textAlign(get())
  }

  fun get() =
    when (this) {
      Left -> processing.core.PConstants.LEFT
      Center -> processing.core.PConstants.CENTER
      Right -> processing.core.PConstants.RIGHT
    }
}

enum class VerticalTextAlign {
  Top,
  Center,
  Bottom,
  Baseline

  ;

  fun apply(applet: processing.core.PApplet) {
    val currentTextAlign = applet.g.textAlign

    applet.textAlign(currentTextAlign, get())
  }

  fun get() =
    when (this) {
      Top -> processing.core.PConstants.TOP
      Center -> processing.core.PConstants.CENTER
      Bottom -> processing.core.PConstants.BOTTOM
      Baseline -> processing.core.PConstants.BASELINE
    }
}
