package dev.wycey.mido.pui.events.mouse.gestures

import dev.wycey.mido.pui.events.EventArgs
import dev.wycey.mido.pui.events.mouse.MouseEventArgs

public data class GestureEventArgs(
  val x: Int,
  val y: Int,
  val prevX: Int,
  val prevY: Int,
  val type: GestureEventType
) : EventArgs() {
  public companion object {
    public fun fromMouseEventArgs(
      e: MouseEventArgs,
      type: GestureEventType
    ): GestureEventArgs = GestureEventArgs(e.mouseX, e.mouseY, e.pmouseX, e.pmouseY, type)
  }

  lateinit var delta: Pair<Float, Float>
}
