package dev.wycey.mido.pui.renderer.gestures

import dev.wycey.mido.pui.bridges.RendererBridge
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.MouseGestureArenaHandler
import dev.wycey.mido.pui.events.mouse.gestures.MouseGesturePredicate
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.renderer.box.ProxyBoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

public class GestureEventRenderer(
  public var eventHandler: (e: GestureEventArgs) -> Unit
) : ProxyBoxRenderer() {
  private var predicate: MouseGesturePredicate? = null
  private var position: Point? = null
  private var neededLayout = false
  private var draws = 0

  private fun persistentDrawCallback() {
    draws++
  }

  override fun performInsertChild() {
    if (predicate == null) {
      predicate =
        MouseGesturePredicate(
          { e ->
            e.delta = e.x - position!!.x to e.y - position!!.y

            eventHandler(e)
          },
          { e ->
            if (draws > 1) return@MouseGesturePredicate false
            if (position == null) return@MouseGesturePredicate false

            val mousePoint = Point(e.mouseX, e.mouseY)

            size.contains(mousePoint - position!!)
          }
        )
    }

    MouseGestureArenaHandler.addPredicate(predicate!!)
    RendererBridge.instance.onPersistentDraw(::persistentDrawCallback)
  }

  override fun performDrop() {
    if (predicate != null) {
      MouseGestureArenaHandler.removePredicate(predicate!!)

      predicate = null
    }

    RendererBridge.instance.removePersistentDraw(::persistentDrawCallback)
  }

  override fun markNeedsLayout() {
    super.markNeedsLayout()

    neededLayout = true
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    draws = 0

    super.paint(d, currentScope)

    if (neededLayout || position == null) {
      position = currentScope.absolutePosition
      neededLayout = false
    }
  }
}
