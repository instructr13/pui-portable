package dev.wycey.mido.pui.components.input

import dev.wycey.mido.pui.bridges.BridgeBase.Companion.applet
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.renderer.BorderRadius
import dev.wycey.mido.pui.util.processing.CursorType

class Checkbox
  @JvmOverloads
  constructor(
    val valueBuilder: () -> Boolean,
    private val label: String? = null,
    private val size: Float = 20f,
    private val borderRadius: BorderRadius = BorderRadius.all(6f),
    private val padding: EdgeInsets = EdgeInsets.all(4f),
    private val checkedFill: Int = 0xff3574f0.toInt(),
    key: String? = null,
    private val onChange: ((Boolean) -> Unit)? = null
  ) : StatefulComponent(key) {
    constructor(
      value: Boolean,
      label: String? = null,
      size: Float = 20f,
      borderRadius: BorderRadius = BorderRadius.all(6f),
      padding: EdgeInsets = EdgeInsets.all(4f),
      checkedFill: Int = 0xff3574f0.toInt(),
      key: String? = null,
      onChange: ((Boolean) -> Unit)? = null
    ) : this({ value }, label, size, borderRadius, padding, checkedFill, key, onChange)

    private fun changeValue(newValue: Boolean) {
      onChange?.invoke(newValue)
    }

    override fun build(context: BuildContext): Component {
      val value = valueBuilder()

      return GestureListener(
        HStack(
          listOf(
            if (!value) {
              Padding(
                padding,
                VirtualBox(
                  Box(
                    borderRadius = borderRadius,
                    fill = 0x00dddddd,
                    stroke = 0xffdddddd.toInt()
                  ),
                  size,
                  size
                )
              )
            } else {
              Padding(
                padding,
                VirtualBox(
                  Box(
                    borderRadius = borderRadius,
                    fill = checkedFill,
                    stroke = 0,
                    strokeWeight = 0f,
                    additionalPaint = { d, _, _ ->
                      d.with(
                        stroke = 0xffffffff.toInt(),
                        strokeWeight = 2f
                      ) {
                        d.line(
                          Point(
                            size / 4,
                            size / 2
                          ),
                          Point(
                            size / 2,
                            size * 3 / 4
                          )
                        )

                        d.line(
                          Point(
                            size / 2,
                            size * 3 / 4
                          ),
                          Point(
                            size * 3 / 4,
                            size / 4
                          )
                        )
                      }
                    }
                  ),
                  size,
                  size
                )
              )
            },
            Text(label ?: "", key = "label")
          ),
          crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Center
        ),
        onHover = { _, _ ->
          CursorType.Hand.apply(applet)
        },
        onLeave = { _, _ ->
          CursorType.Arrow.apply(applet)
        },
        onClick = { _, type ->
          if (type.button == MouseButtons.LEFT) changeValue(!value)
        }
      )
    }
  }
