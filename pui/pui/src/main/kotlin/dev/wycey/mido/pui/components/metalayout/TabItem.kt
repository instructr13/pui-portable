package dev.wycey.mido.pui.components.metalayout

import dev.wycey.mido.pui.bridges.BridgeBase
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.components.text.TextStyle
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.BorderRadius
import dev.wycey.mido.pui.util.processing.CursorType

internal class TabItem(
  private val title: String,
  private val selected: Boolean = false,
  private val onSelect: () -> Unit = {},
  private val disabled: Boolean = false,
  key: String? = null
) : StatefulComponent(key) {
  override fun build(context: BuildContext): Component {
    var pressing by signal(false)
    var hovering by signal(false)
    val selectedLineFill = (if (selected) 0xfffd8c73 else 0xffd8dee4).toInt()
    val lineHeight = if (selected) 2f else 1f

    return GestureListener(
      onPress = onPress@{ _, type ->
        if (type.button != MouseButtons.LEFT || disabled) return@onPress

        pressing = true
      },
      onClick = onClick@{ _, type ->
        if (type.button != MouseButtons.LEFT || disabled) return@onClick

        onSelect()
      },
      onRelease = onRelease@{ _, type ->
        if (type.button != MouseButtons.LEFT || disabled) return@onRelease

        pressing = false
      },
      onHover = onHover@{ _, _ ->
        if (disabled) return@onHover

        hovering = true

        CursorType.Hand.apply(BridgeBase.applet)
      },
      onLeave = { _, _ ->
        if (!disabled) {
          hovering = false
        }

        CursorType.Arrow.apply(BridgeBase.applet)
      },
      child =
        Box(
          additionalPaint = { d, _, size ->
            d.with(fill = selectedLineFill, stroke = 0x00ffffff) {
              d.rect(Point(0f, size.height - lineHeight), Size(size.width, lineHeight))
            }
          },
          child =
            Padding(
              EdgeInsets.symmetric(8f, 2f),
              Box(
                fill =
                  (
                    if (pressing) {
                      0xffafafaf
                    } else if (hovering) {
                      0xffd9d9d9
                    } else {
                      0xffffffff
                    }
                  ).toInt(),
                borderRadius = BorderRadius.all(2f),
                child =
                  Padding(
                    EdgeInsets.symmetric(4f, 6f),
                    Text(
                      title,
                      TextStyle(color = (if (disabled) 0xffa0a0a0 else 0xff000000).toInt())
                    )
                  )
              )
            )
        )
    )
  }
}
