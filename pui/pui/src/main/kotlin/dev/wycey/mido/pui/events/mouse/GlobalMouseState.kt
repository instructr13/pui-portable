package dev.wycey.mido.pui.events.mouse

internal class GlobalMousePointer {
  var dragging = false
  var pressing = false
}

internal object GlobalMouseState {
  val pointer = listOf(GlobalMousePointer())
}
