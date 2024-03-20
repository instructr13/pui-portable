package dev.wycey.mido.leinwand.draw

import dev.wycey.mido.pui.events.mouse.gestures.GestureEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventType

interface Clickable {
  fun onClick(
    e: GestureEventArgs,
    type: GestureEventType.Click
  )
}
