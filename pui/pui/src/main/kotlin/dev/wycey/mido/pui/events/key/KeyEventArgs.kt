package dev.wycey.mido.pui.events.key

import dev.wycey.mido.pui.events.EventArgs
import processing.event.KeyEvent

data class KeyEventArgs(val key: Char, val keyCode: Int, val isAutoRepeat: Boolean) : EventArgs() {
  companion object {
    @JvmStatic
    fun fromKeyEvent(e: KeyEvent) = KeyEventArgs(e.key, e.keyCode, e.isAutoRepeat)
  }
}
