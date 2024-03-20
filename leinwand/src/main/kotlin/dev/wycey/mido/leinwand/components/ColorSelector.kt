package dev.wycey.mido.leinwand.components

import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.components.layout.*
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.util.round

class ColorSelector(private val instanceId: Int) : StatefulComponent("colorSelector$instanceId") {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!

    return HStack(
      listOf(
        GestureListener(
          onClick = onClick@{ _, type ->
            if (type.button != MouseButtons.LEFT) return@onClick

            handle.selectingForeground = !handle.selectingForeground
          },
          child =
            VirtualBox(
              height = 48f,
              width = 48f,
              child =
                ZStack(
                  {
                    val list = mutableListOf<Component>()

                    val foregroundPalette =
                      Positioned(
                        left = 0f,
                        top = 0f,
                        child =
                          VirtualBox(
                            width = 30f,
                            height = 30f,
                            child = ColorPalette(handle.foregroundColor)
                          )
                      )

                    val backgroundPalette =
                      Positioned(
                        right = 0f,
                        bottom = 0f,
                        child =
                          VirtualBox(
                            width = 30f,
                            height = 30f,
                            child = ColorPalette(handle.backgroundColor)
                          )
                      )

                    if (handle.selectingForeground) {
                      list.add(backgroundPalette)
                      list.add(foregroundPalette)
                    } else {
                      list.add(foregroundPalette)
                      list.add(backgroundPalette)
                    }

                    list
                  }
                )
            )
        ),
        VirtualBox(width = 10f),
        VStack(
          listOf(
            Text(
              if (handle.selectingForeground) "Foreground" else "Background",
              Styles.text,
              key = "colorPickerLabel${handle.selectingForeground}"
            ),
            VirtualBox(height = 4f),
            Text(
              Styles.text,
              key = "colorPickerRGBALabel${handle.selectingForeground}"
            ) {
              "RGBA: ${
                (if (handle.selectingForeground) handle.foregroundColor else handle.backgroundColor).toSRGB().toHex()
              }\n" +
                "Alpha: ${
                  (
                    (if (handle.selectingForeground) handle.foregroundColor else handle.backgroundColor).alpha * 100
                  ).round(
                    2
                  )
                }%"
            }
          )
        )
      )
    )
  }
}
