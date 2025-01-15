package dev.wycey.mido.leinwand.components.tools

import dev.wycey.mido.fraiselait.state.StateManager
import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.leinwand.tools.brush.Brush
import dev.wycey.mido.pui.bridges.RendererBridge
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.input.Checkbox
import dev.wycey.mido.pui.components.input.Slider
import dev.wycey.mido.pui.components.layout.*
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.layout.Offstage
import dev.wycey.mido.pui.state.signals.onDisposeEffect
import dev.wycey.mido.pui.util.processing.RenderMode

internal class BrushSizeSettings(private val brush: Brush, key: String? = null) : StatefulComponent(key) {
  override fun build(context: BuildContext): Component {
    var size by signal(brush.size)
    var usesLightSensorAsBrushSize by signal(false)

    val callback =
      createFunction {
        val newSize = (StateManager.state!!.lightStrength / 500f * 40f).toInt()

        if (newSize != size) {
          size = newSize

          brush.size = newSize
        }
      }

    effect {
      if (usesLightSensorAsBrushSize) {
        if (StateManager.state == null) {
          usesLightSensorAsBrushSize = false

          return@effect
        }

        RendererBridge.instance.onPersistentDraw(callback)

        onDisposeEffect {
          RendererBridge.instance.removePersistentDraw(callback)
        }
      }
    }

    return VStack(
      listOf(
        HStack(
          listOf(
            VirtualBox(
              width = 80f,
              child =
                Center(
                  child =
                    Box(
                      child = VirtualBox(width = size.toFloat(), height = size.toFloat()),
                      mode = RenderMode.Center,
                      additionalPaint = { d, _, _ ->
                        val image = brush.defaultCreateCursor(size).first

                        d.applet.image(image, 0f, 0f)
                      }
                    )
                )
            ),
            Expanded(
              VStack(
                listOf(
                  Padding(
                    EdgeInsets.only(left = 8f),
                    child = Text("Brush Size: ${size}px", Styles.text)
                  ),
                  Slider(
                    { size.toFloat() },
                    min = 1f,
                    max = 40f,
                    barSize = 180f,
                    barColor = 0xff4d4e51.toInt(),
                    trackColor = 0xff3574f0.toInt(),
                    additionalPadding = EdgeInsets.all(18f),
                    onChange = {
                      size = it.toInt()

                      brush.size = it.toInt()
                    }
                  )
                )
              )
            )
          )
        ),
        Offstage(
          offstage = StateManager.state == null,
          child =
            Padding(
              EdgeInsets.only(top = 8f),
              child =
                HStack(
                  listOf(
                    Checkbox(
                      { usesLightSensorAsBrushSize },
                      "",
                      onChange = {
                        if (!it) {
                          RendererBridge.instance.removePersistentDraw(callback)
                        }

                        usesLightSensorAsBrushSize = it
                      }
                    ),
                    Text("光センサ筆圧制御", Styles.text)
                  ),
                  crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Center
                )
            )
        )
      )
    )
  }
}
