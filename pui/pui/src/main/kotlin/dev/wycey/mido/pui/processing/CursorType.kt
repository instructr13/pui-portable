package dev.wycey.mido.pui.util.processing

enum class CursorType {
  Arrow,
  Cross,
  Hand,
  Move,
  Text,
  Wait

  ;

  fun apply(applet: processing.core.PApplet) {
    applet.cursor(get())
  }

  fun get() =
    when (this) {
      Arrow -> processing.core.PConstants.ARROW
      Cross -> processing.core.PConstants.CROSS
      Hand -> processing.core.PConstants.HAND
      Move -> processing.core.PConstants.MOVE
      Text -> processing.core.PConstants.TEXT
      Wait -> processing.core.PConstants.WAIT
    }
}
