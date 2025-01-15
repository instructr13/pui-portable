package dev.wycey.mido.pui.util.processing

import processing.core.PApplet
import processing.core.PConstants

public enum class RenderModeWithoutRadius {
  Corner,
  Corners,
  Center

  ;

  public fun apply(applet: PApplet) {
    applet.rectMode(get())
  }

  public fun get(): Int =
    when (this) {
      Corner -> PConstants.CORNER
      Corners -> PConstants.CORNERS
      Center -> PConstants.CENTER
    }
}
