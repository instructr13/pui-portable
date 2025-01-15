package dev.wycey.mido.pui.components.gestures

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventType
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.gestures.GestureEventRenderer

public class GestureListener
  @JvmOverloads
  constructor(
    child: Component,
    private val onPress: ((e: GestureEventArgs, type: GestureEventType.Press) -> Unit)? = null,
    private val onRelease: ((e: GestureEventArgs, type: GestureEventType.Release) -> Unit)? = null,
    private val onClick: ((e: GestureEventArgs, type: GestureEventType.Click) -> Unit)? = null,
    private val onDrag: ((e: GestureEventArgs, type: GestureEventType.Drag) -> Unit)? = null,
    private val onDrop: ((e: GestureEventArgs, type: GestureEventType.Drop) -> Unit)? = null,
    private val onHover: ((e: GestureEventArgs, type: GestureEventType.Hover) -> Unit)? = null,
    private val onLeave: ((e: GestureEventArgs, type: GestureEventType.Leave) -> Unit)? = null,
    private val onWheel: ((e: GestureEventArgs, type: GestureEventType.Wheel) -> Unit)? = null,
    key: String? = null
  ) : SingleChildRendererComponent(key, child) {
    private fun createEventHandler(): (GestureEventArgs) -> Unit =
      { e ->
        when (e.type) {
          is GestureEventType.Press -> onPress?.invoke(e, e.type)
          is GestureEventType.Release -> onRelease?.invoke(e, e.type)
          is GestureEventType.Click -> onClick?.invoke(e, e.type)
          is GestureEventType.Drag -> onDrag?.invoke(e, e.type)
          is GestureEventType.Drop -> onDrop?.invoke(e, e.type)
          is GestureEventType.Hover -> onHover?.invoke(e, e.type)
          is GestureEventType.Leave -> onLeave?.invoke(e, e.type)
          is GestureEventType.Wheel -> onWheel?.invoke(e, e.type)
        }
      }

    override fun createRenderer(context: BuildContext): GestureEventRenderer =
      GestureEventRenderer(
        createEventHandler()
      )

    override fun updateRenderer(
      context: BuildContext,
      renderer: RendererObject
    ) {
      if (renderer !is GestureEventRenderer) return

      renderer.eventHandler = createEventHandler()
    }
  }
