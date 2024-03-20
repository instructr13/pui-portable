package dev.wycey.mido.pui.util.processing

enum class RenderMode {
  Corner,
  Corners,
  Center,
  Radius

  ;

  fun get() =
    when (this) {
      Corner -> processing.core.PConstants.CORNER
      Corners -> processing.core.PConstants.CORNERS
      Center -> processing.core.PConstants.CENTER
      Radius -> processing.core.PConstants.RADIUS
    }
}
