package dev.wycey.mido.pui.util.processing

import processing.core.PApplet
import processing.core.PConstants

public enum class StrokeCaps {
  Round,
  Square,
  Project

  ;

  public fun apply(applet: PApplet) {
    applet.strokeCap(get())
  }

  public fun get(): Int =
    when (this) {
      Round -> PConstants.ROUND
      Square -> PConstants.SQUARE
      Project -> PConstants.PROJECT
    }
}
