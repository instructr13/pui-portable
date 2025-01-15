package dev.wycey.mido.leinwand.components

import dev.wycey.mido.leinwand.components.DrawingRoot.applet
import dev.wycey.mido.leinwand.draw.Draggable
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.util.processing.CursorType

internal class Canvas(
  private val instanceId: Int,
  private val initialSize: Size = Size(600f, 600f),
  key: String? = null
) :
  StatelessComponent(key) {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!

    return GestureListener(
      onPress = onPress@{ e, type ->
        if (type.button != MouseButtons.LEFT) return@onPress

        if (handle.currentTool is Draggable) {
          (handle.currentTool as Draggable).onPress(e, type)
        }
      },
      onDrag = onDrag@{ e, type ->
        if (type.button != MouseButtons.LEFT || e.delta.first < 0 || e.delta.second < 0 ||
          e.delta.first > initialSize.width || e.delta.second > initialSize.height
        ) {
          return@onDrag
        }

        if (handle.currentTool is Draggable) {
          (handle.currentTool as Draggable).onDrag(e, type)
        }
      },
      onRelease = onRelease@{ e, type ->
        if (type.button != MouseButtons.LEFT) return@onRelease

        if (handle.currentTool is Draggable) {
          (handle.currentTool as Draggable).onRelease(e, type)
        }
      },
      onHover = onHover@{ _, _ ->
        handle.currentTool.applyCursor(applet)
      },
      onLeave = onLeave@{ _, _ ->
        CursorType.Arrow.apply(applet)
      },
      child = RawCanvas(initialSize, instanceId)
    )
  }
}
