package dev.wycey.mido.pui.util.processing

import processing.core.PApplet
import processing.core.PConstants

public enum class TextAlign {
  Left,
  Center,
  Right

  ;

  public fun apply(applet: PApplet) {
    applet.textAlign(get())
  }

  public fun get(): Int =
    when (this) {
      Left -> PConstants.LEFT
      Center -> PConstants.CENTER
      Right -> PConstants.RIGHT
    }
}

public enum class VerticalTextAlign {
  Top,
  Center,
  Bottom,
  Baseline

  ;

  public fun apply(applet: PApplet) {
    val currentTextAlign = applet.g.textAlign

    applet.textAlign(currentTextAlign, get())
  }

  public fun get(): Int =
    when (this) {
      Top -> PConstants.TOP
      Center -> PConstants.CENTER
      Bottom -> PConstants.BOTTOM
      Baseline -> PConstants.BASELINE
    }
}
