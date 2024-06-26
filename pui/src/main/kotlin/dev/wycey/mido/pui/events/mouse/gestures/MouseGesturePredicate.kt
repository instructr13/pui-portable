package dev.wycey.mido.pui.events.mouse.gestures

import dev.wycey.mido.pui.events.mouse.MouseEventArgs

typealias GestureEventRoute = (e: GestureEventArgs) -> Unit

open class MouseGesturePredicate(val route: GestureEventRoute, val body: (e: MouseEventArgs) -> Boolean) {
  open operator fun invoke(e: MouseEventArgs) = body(e)
}
