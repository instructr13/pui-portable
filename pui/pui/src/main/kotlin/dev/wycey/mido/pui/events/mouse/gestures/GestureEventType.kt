package dev.wycey.mido.pui.events.mouse.gestures

import dev.wycey.mido.pui.events.mouse.MouseWheelType
import dev.wycey.mido.pui.layout.Point
import kotlin.time.Duration

public sealed class GestureEventType {
  public data class Press(val button: Int) : GestureEventType()

  public data class Release(val button: Int) : GestureEventType()

  public data class Click(val button: Int, val count: Int) : GestureEventType()

  public data class Drag(val button: Int, val startingPoint: Point, val persistent: Boolean = false) :
    GestureEventType()

  public data class Drop(val button: Int, val cancelled: Boolean = false) : GestureEventType()

  public data class Hover(
    val duration: Duration,
    val persistent: Boolean = false
  ) :
    GestureEventType()

  public data object Leave : GestureEventType()

  public data class Wheel(val type: MouseWheelType, val count: Int) : GestureEventType()

  override fun toString(): String =
    when (this) {
      is Press -> "press"
      is Release -> "release"
      is Click -> "click${if (count > 1) " ($count)" else ""}"
      is Drag -> "drag${if (persistent) " (persistent)" else ""}"
      is Drop -> "drop${if (cancelled) " (cancelled)" else ""}"
      is Hover -> "hover${if (persistent) " (persistent)" else ""}"
      is Leave -> "leave"
      is Wheel ->
        "wheel " +
          when (type) {
            MouseWheelType.Up -> "up"
            MouseWheelType.Down -> "down"
          } + " ($count)"
    }
}
