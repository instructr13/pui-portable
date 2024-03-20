package dev.wycey.mido.pui.util.processing

enum class StrokeCaps {
  Round,
  Square,
  Project

  ;

  fun apply(applet: processing.core.PApplet) {
    applet.strokeCap(get())
  }

  fun get() =
    when (this) {
      Round -> processing.core.PConstants.ROUND
      Square -> processing.core.PConstants.SQUARE
      Project -> processing.core.PConstants.PROJECT
    }
}
