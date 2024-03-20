package dev.wycey.mido.pui.examples

import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.components.text.TextStyle
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets

class SimpleChip
  @JvmOverloads
  constructor(
    private val title: String,
    private val body: String,
    private val fontSize: Float = 12f,
    private val chipForegroundColor: Int = 0xffffffff.toInt(),
    private val chipBackgroundColor: Int = 0xff5886e0.toInt(),
    private val bodyForegroundColor: Int = 0xff000000.toInt(),
    key: String? = null
  ) : StatelessComponent(key) {
    override fun build(context: BuildContext) =
      HStack(
        listOf(
          Box(
            Padding(
              EdgeInsets.symmetric(4f, 4f),
              Text(
                title,
                TextStyle(
                  fontSize = fontSize,
                  color = chipForegroundColor
                )
              )
            ),
            fill = chipBackgroundColor
          ),
          Box(
            Padding(
              EdgeInsets.symmetric(4f, 4f),
              Text(
                body,
                TextStyle(
                  fontSize = fontSize,
                  color = bodyForegroundColor
                )
              )
            )
          )
        )
      )
  }
