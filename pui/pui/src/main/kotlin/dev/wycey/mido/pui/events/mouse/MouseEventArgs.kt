package dev.wycey.mido.pui.events.mouse

import dev.wycey.mido.pui.events.EventArgs
import processing.event.MouseEvent

public data class MouseEventArgs(
  val mouseX: Int,
  val mouseY: Int,
  val pmouseX: Int,
  val pmouseY: Int,
  val button: Int,
  val action: Int,
  val count: Int
) : EventArgs()

public fun MouseEvent.toEventArgs(
  pmouseX: Int,
  pmouseY: Int
): MouseEventArgs =
  MouseEventArgs(
    this.x,
    this.y,
    pmouseX,
    pmouseY,
    button,
    action,
    count
  )
