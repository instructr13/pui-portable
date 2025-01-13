package dev.wycey.mido.pui.examples.colors

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.layout.Point

/**
 * A color picker box for the HSLuv color space.
 */
internal class HSLuvColorPickerBox(
  private val onSetColor: (s: Float, l: Float) -> Unit,
  private val initialColor: HSLuv,
  private val size: Int,
  private val hue: Float,
  private val pickedColorCircleRadius: Float,
  key: String? = null
) : StatefulComponent(key) {
  private fun colorToCoordinate(color: HSLuv): Point {
    val x = color.s / 100f * size
    val y = size - color.l / 100f * size

    return Point(x, y)
  }

  override fun build(context: BuildContext): Component {
    var coordinate by signal(colorToCoordinate(initialColor))

    return VirtualBox(
      GestureListener(
        onPress = onPress@{ e, type ->
          if (type.button != MouseButtons.LEFT) return@onPress

          val x = e.delta.first
          val y = e.delta.second

          onSetColor((x / size) * 100, (1 - y / size) * 100)

          coordinate = Point(x, y)
        },
        onDrag = onDrag@{ e, type ->
          if (type.button != MouseButtons.LEFT) return@onDrag

          val x = e.delta.first.coerceIn(0f..size.toFloat())
          val y = e.delta.second.coerceIn(0f..size.toFloat())

          onSetColor((x / size) * 100, (1 - y / size) * 100)

          coordinate = Point(x, y)
        },
        child = HSLuvRenderColorPickerBox(coordinate, hue, pickedColorCircleRadius)
      ),
      size.toFloat(),
      size.toFloat()
    )
  }
}
