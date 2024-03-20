package dev.wycey.mido.pui.util.processing

enum class StrokeJoins {
  Round,
  Bevel,
  Miter

  ;

  fun apply(applet: processing.core.PApplet) {
    applet.strokeJoin(get())
  }

  fun get() =
    when (this) {
      Round -> processing.core.PConstants.ROUND
      Bevel -> processing.core.PConstants.BEVEL
      Miter -> processing.core.PConstants.MITER
    }
}
