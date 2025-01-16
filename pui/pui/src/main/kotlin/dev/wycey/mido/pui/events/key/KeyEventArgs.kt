package dev.wycey.mido.pui.events.key

import dev.wycey.mido.pui.events.EventArgs
import processing.event.KeyEvent

public data class KeyEventArgs(
  val key: Char,
  val keyCode: Int,
  val isAutoRepeat: Boolean
) : EventArgs() {
  public companion object {
    @JvmStatic
    public fun fromKeyEvent(e: KeyEvent): KeyEventArgs = KeyEventArgs(e.key, e.keyCode, e.isAutoRepeat)
  }
}
