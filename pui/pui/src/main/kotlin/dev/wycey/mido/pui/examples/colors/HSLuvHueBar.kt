package dev.wycey.mido.pui.examples.colors

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventType
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.state.signals.untracked

internal class HSLuvHueBar(
  private val onSetHue: (hue: Float) -> Unit,
  private val initialHue: Float,
  private val s: Float,
  private val l: Float,
  private val size: Size,
  key: String? = null
) : StatefulComponent(key) {
  private fun hueToY(hue: Float): Float = size.height - hue / 360f * size.height

  private fun onSelect(
    e: GestureEventArgs,
    type: GestureEventType,
    currentY: Float
  ): Float {
    when (type) {
      is GestureEventType.Press -> {
        if (type.button != MouseButtons.LEFT) return currentY
      }

      is GestureEventType.Drag -> {
        if (type.button != MouseButtons.LEFT) return currentY
      }

      else -> {}
    }

    val newY = e.delta.second.coerceIn(0f..size.height)
    val newHue = (1f - newY / size.height) * 360f

    onSetHue(newHue)

    return newY
  }

  override fun build(context: BuildContext): Component {
    var y by signal(hueToY(initialHue))
    val h by computed { y / size.height * 360f }

    return GestureListener(
      onPress = onPress@{ e, type ->
        y = onSelect(e, type, untracked { y })
      },
      onDrag = onDrag@{ e, type ->
        y = onSelect(e, type, untracked { y })
      },
      child =
        VirtualBox(
          width = size.width,
          height = size.height,
          child = HSLuvRenderHueBar(h, s, l)
        )
    )
  }
}
