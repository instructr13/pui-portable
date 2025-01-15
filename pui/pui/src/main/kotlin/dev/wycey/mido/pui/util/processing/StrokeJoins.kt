package dev.wycey.mido.pui.util.processing

import processing.core.PApplet
import processing.core.PConstants

public enum class StrokeJoins {
  Round,
  Bevel,
  Miter

  ;

  public fun apply(applet: PApplet) {
    applet.strokeJoin(get())
  }

  public fun get(): Int =
    when (this) {
      Round -> PConstants.ROUND
      Bevel -> PConstants.BEVEL
      Miter -> PConstants.MITER
    }
}
