package dev.wycey.mido.pui.util.processing

import processing.core.PApplet

public enum class CursorType {
  Arrow,
  Cross,
  Hand,
  Move,
  Text,
  Wait

  ;

  public fun apply(applet: PApplet) {
    applet.cursor(get())
  }

  public fun get(): Int =
    when (this) {
      Arrow -> processing.core.PConstants.ARROW
      Cross -> processing.core.PConstants.CROSS
      Hand -> processing.core.PConstants.HAND
      Move -> processing.core.PConstants.MOVE
      Text -> processing.core.PConstants.TEXT
      Wait -> processing.core.PConstants.WAIT
    }
}
