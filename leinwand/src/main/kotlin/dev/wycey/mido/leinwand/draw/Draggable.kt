package dev.wycey.mido.leinwand.draw

import dev.wycey.mido.pui.events.mouse.gestures.GestureEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventType

interface Draggable {
  fun onPress(
    e: GestureEventArgs,
    type: GestureEventType.Press
  )

  fun onDrag(
    e: GestureEventArgs,
    type: GestureEventType.Drag
  )

  fun onRelease(
    e: GestureEventArgs,
    type: GestureEventType.Release
  )
}
