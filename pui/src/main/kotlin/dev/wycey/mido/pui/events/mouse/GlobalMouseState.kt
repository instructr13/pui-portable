package dev.wycey.mido.pui.events.mouse

class GlobalMousePointer {
  var dragging = false
  var pressing = false
}

object GlobalMouseState {
  val pointer = listOf(GlobalMousePointer())
}
