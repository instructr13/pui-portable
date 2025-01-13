package dev.wycey.mido.pui.events.mouse.gestures

import dev.wycey.mido.pui.events.mouse.MouseEventArgs

internal class MouseGestureArena(
  private val predicates: List<MouseGesturePredicate>
) {
  private var wonPredicate: MouseGesturePredicate? = null

  val winner: GestureEventRoute?
    get() = wonPredicate?.route

  fun startWar(e: MouseEventArgs): Boolean {
    wonPredicate = null

    for (predicate in predicates.asReversed()) {
      if (predicate(e)) {
        wonPredicate = predicate

        return true
      }
    }

    return false
  }

  fun recheckCurrent(e: MouseEventArgs): Boolean {
    if (wonPredicate == null) {
      return false
    }

    if (wonPredicate!!(e)) {
      return true
    }

    startWar(e)

    return false
  }
}
