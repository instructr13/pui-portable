package dev.wycey.mido.pui.bridges

import dev.wycey.mido.pui.events.mouse.GlobalMouseState
import dev.wycey.mido.pui.events.mouse.MouseActions
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.events.mouse.MouseEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.*

interface MouseEventBridgeContract {
  fun translateMouseEvent(e: MouseEventArgs)

  fun handleMouseWheel(e: MouseEventArgs)
}

class MouseEventBridge internal constructor() : MouseEventBridgeContract,
  BridgeBaseWithAutoInit() {
    companion object {
      @JvmField
      var instanceNullable: MouseEventBridge? = null

      @JvmStatic
      val instance get() = checkInstance(instanceNullable)
    }

    override fun initInstance() {
      super.initInstance()

      instanceNullable = this
      applet.fill(0)

      println("Mouse event bridge initialized")
    }

    override fun translateMouseEvent(e: MouseEventArgs) {
      val pointer = GlobalMouseState.pointer[0]

      // ONESHOT GESTURES

      if (MouseGestureArenaHandler.tracking != null && e.action == MouseActions.CLICK) {
        MouseGestureArenaHandler.handleOneshotGesture(pointer, OneshotGestureType.Click, e)

        return
      }

      if (e.action == MouseActions.EXIT) {
        MouseGestureArenaHandler.handleOneshotGesture(pointer, OneshotGestureType.Exit, e)

        return
      }

      // INITIAL GESTURES

      if (e.action == MouseActions.PRESS &&
        MouseGestureArenaHandler.handleInitialGesture(
          pointer,
          InitialGestureType.Press,
          e
        )
      ) {
        return
      }

      if (e.action == MouseActions.MOVE &&
        MouseGestureArenaHandler.handleInitialGesture(
          pointer,
          InitialGestureType.MaybeHover,
          e
        )
      ) {
        return
      }

      // TRACKING GESTURES

      if (MouseGestureArenaHandler.tracking == null) return

      if (e.action == MouseActions.DRAG) {
        MouseGestureArenaHandler.handleTrackingGesture(pointer, TrackingGestureType.Drag, e)

        return
      }

      if (e.action == MouseActions.MOVE) {
        MouseGestureArenaHandler.handleTrackingGesture(pointer, TrackingGestureType.Hover, e)

        return
      }

      // FINAL GESTURES

      if (e.action == MouseActions.RELEASE) {
        MouseGestureArenaHandler.handleFinalGesture(pointer, FinalGestureType.Release, e)

        return
      }

      // FinalGestureType.Left is handled by the OneshotGestureType.Exit or TrackingGestureType.Hover handlers
    }

    override fun handleMouseWheel(e: MouseEventArgs) {
      if (MouseGestureArenaHandler.tracking == null) return

      val pointer = GlobalMouseState.pointer[0]

      if (pointer.pressing && e.button != MouseButtons.CENTER) return

      if (e.count > 0) {
        MouseGestureArenaHandler.handleOneshotGesture(pointer, OneshotGestureType.WheelDown, e)
      } else if (e.count < 0) {
        MouseGestureArenaHandler.handleOneshotGesture(pointer, OneshotGestureType.WheelUp, e)
      }
    }
  }
