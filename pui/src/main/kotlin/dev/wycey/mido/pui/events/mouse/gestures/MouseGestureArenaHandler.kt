package dev.wycey.mido.pui.events.mouse.gestures

import dev.wycey.mido.pui.events.mouse.GlobalMousePointer
import dev.wycey.mido.pui.events.mouse.MouseEventArgs
import dev.wycey.mido.pui.events.mouse.MouseWheelType
import dev.wycey.mido.pui.layout.Point
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object MouseGestureArenaHandler {
  private val availablePredicates = mutableSetOf<MouseGesturePredicate>()
  private var arenas = mutableMapOf<GlobalMousePointer, MouseGestureArena>()

  var tracking: GestureEventRoute? = null

  private var hoveringSince: Duration = Duration.ZERO
  private var dragStartingPoint: Point = Point.ZERO

  private lateinit var trackingClick: ClickGestureContext

  fun addPredicate(predictor: MouseGesturePredicate) {
    availablePredicates.add(predictor)
  }

  fun removePredicate(predictor: MouseGesturePredicate) {
    availablePredicates.remove(predictor)
  }

  private fun createArena(pointer: GlobalMousePointer): MouseGestureArena =
    arenas.computeIfAbsent(pointer) {
      MouseGestureArena(
        availablePredicates.toList()
      )
    }

  private fun sweepArena(pointer: GlobalMousePointer) {
    arenas.remove(pointer)
  }

  private fun trackRoute(route: GestureEventRoute) {
    if (tracking != null) {
      tracking = null
    }

    tracking = route
  }

  private fun untrackRoute(route: GestureEventRoute) {
    if (tracking == route) {
      tracking = null
    }
  }

  fun handleInitialGesture(
    pointer: GlobalMousePointer,
    type: InitialGestureType,
    e: MouseEventArgs
  ): Boolean {
    if (tracking == null) {
      val arena = createArena(pointer)

      if (arena.winner == null && !arena.startWar(e)) {
        sweepArena(pointer)

        return true
      }

      trackRoute(arena.winner!!)
    }

    return when (type) {
      InitialGestureType.Press -> handlePress(pointer, e)
      InitialGestureType.MaybeHover -> handleMaybeHover(pointer, e)
    }
  }

  private fun handlePress(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ): Boolean {
    assert(tracking != null)

    if (!::trackingClick.isInitialized) {
      trackingClick = ClickGestureContext(tracking!!, e.count)
    }

    handleGestureEvent(e, GestureEventType.Press(e.button))

    return true
  }

  private fun handleMaybeHover(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ): Boolean {
    if (tracking != null) return false

    hoveringSince = System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)

    handleGestureEvent(e, GestureEventType.Hover(Duration.ZERO))

    return true
  }

  fun handleOneshotGesture(
    pointer: GlobalMousePointer,
    type: OneshotGestureType,
    e: MouseEventArgs
  ) = when (type) {
    OneshotGestureType.Click -> handleClick(pointer, e)
    OneshotGestureType.Exit -> handleOnExit(pointer, e)
    OneshotGestureType.WheelUp -> handleWheel(pointer, e, MouseWheelType.Up)
    OneshotGestureType.WheelDown -> handleWheel(pointer, e, MouseWheelType.Down)
  }

  private fun handleClick(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ) {
    assert(tracking != null)

    if (trackingClick.route != tracking) {
      trackingClick.route = tracking!!
      trackingClick.initialCount = e.count
    }

    if (trackingClick.initialCount > e.count) {
      trackingClick.initialCount = e.count
    }

    handleGestureEvent(e, GestureEventType.Click(e.button, e.count - trackingClick.initialCount))
  }

  private fun handleOnExit(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ) {
    if (tracking == null) return

    if (pointer.dragging) {
      pointer.dragging = false

      handleGestureEvent(e, GestureEventType.Drop(e.button, true))
      handleGestureEvent(e, GestureEventType.Release(e.button))
    }

    if (pointer.pressing) {
      pointer.pressing = false

      handleReleaseWithoutSweep(pointer, e)
    }

    handleFinalGesture(pointer, FinalGestureType.Left, e)
  }

  private fun handleWheel(
    pointer: GlobalMousePointer,
    e: MouseEventArgs,
    type: MouseWheelType
  ) {
    assert(tracking != null)

    handleGestureEvent(e, GestureEventType.Wheel(type, e.count))
  }

  fun handleTrackingGesture(
    pointer: GlobalMousePointer,
    type: TrackingGestureType,
    e: MouseEventArgs
  ) {
    assert(tracking != null)

    return when (type) {
      TrackingGestureType.Drag -> handleDrag(pointer, e)
      TrackingGestureType.Hover -> handleHover(pointer, e)
    }
  }

  private fun handleDrag(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ) {
    if (!pointer.dragging) {
      pointer.dragging = true

      dragStartingPoint = Point(e.mouseX, e.mouseY)

      handleGestureEvent(e, GestureEventType.Drag(e.button, dragStartingPoint))

      return
    }

    handleGestureEvent(e, GestureEventType.Drag(e.button, dragStartingPoint, true))
  }

  private fun handleHover(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ) {
    val arena = arenas[pointer]!!

    if (!arena.recheckCurrent(e)) {
      handleFinalGesture(pointer, FinalGestureType.Left, e)

      return
    }

    handleGestureEvent(
      e,
      GestureEventType.Hover(
        System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS) - hoveringSince,
        true
      )
    )
  }

  fun handleFinalGesture(
    pointer: GlobalMousePointer,
    type: FinalGestureType,
    e: MouseEventArgs
  ) {
    assert(tracking != null)

    return when (type) {
      FinalGestureType.Release -> handleRelease(pointer, e)
      FinalGestureType.Left -> handleLeft(pointer, e)
    }
  }

  fun handleReleaseWithoutSweep(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ) {
    if (tracking == null) return

    handleGestureEvent(e, GestureEventType.Release(e.button))
  }

  fun handleRelease(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ) {
    handleGestureEvent(e, GestureEventType.Release(e.button))

    val arena = arenas[pointer]!!

    if (pointer.dragging) {
      pointer.dragging = false

      dragStartingPoint = Point.ZERO

      handleGestureEvent(e, GestureEventType.Drop(e.button, false))

      if (!arena.recheckCurrent(e)) {
        handleFinalGesture(pointer, FinalGestureType.Left, e)
      }
    }
  }

  fun handleLeft(
    pointer: GlobalMousePointer,
    e: MouseEventArgs
  ) {
    handleGestureEvent(e, GestureEventType.Leave)

    hoveringSince = Duration.ZERO

    untrackRoute(tracking!!)
    sweepArena(pointer)
  }

  private fun handleGestureEvent(
    e: MouseEventArgs,
    type: GestureEventType
  ) {
    GestureEventArgs.fromMouseEventArgs(e, type).let(tracking!!)
  }
}
