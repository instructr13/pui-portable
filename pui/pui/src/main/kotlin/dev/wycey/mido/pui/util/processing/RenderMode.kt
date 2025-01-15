package dev.wycey.mido.pui.util.processing

import processing.core.PConstants

public enum class RenderMode {
  Corner,
  Corners,
  Center,
  Radius

  ;

  public fun get(): Int =
    when (this) {
      Corner -> PConstants.CORNER
      Corners -> PConstants.CORNERS
      Center -> PConstants.CENTER
      Radius -> PConstants.RADIUS
    }
}
