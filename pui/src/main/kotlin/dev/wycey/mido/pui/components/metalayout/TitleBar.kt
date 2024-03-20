package dev.wycey.mido.pui.components.metalayout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.layout.Expanded
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.components.text.TextStyle
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets

class TitleBar
  @JvmOverloads
  constructor(
    private val title: String,
    private val subtitle: String? = null,
    private val backgroundColor: Int = 0xffefefef.toInt(),
    private val subtitleColor: Int = 0xff888888.toInt(),
    private val additionalArea: (BuildContext) -> Array<Component> = { emptyArray() },
    key: String? = null
  ) : StatelessComponent(key) {
    override fun build(context: BuildContext) =
      Box(
        Padding(
          EdgeInsets.symmetric(20f, 20f),
          HStack(
            listOf(
              Expanded(
                VStack(
                  {
                    val titleText = Text(title, TextStyle(fontSize = 30f))

                    if (subtitle != null) {
                      listOf(
                        titleText,
                        Padding(
                          EdgeInsets.only(top = 2f, left = 4f),
                          Text(
                            subtitle,
                            TextStyle(
                              fontSize = 14f,
                              color = subtitleColor
                            )
                          )
                        )
                      )
                    } else {
                      listOf(titleText)
                    }
                  }
                )
              ),
              HStack(
                additionalArea(context).asList()
              )
            ),
            crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Center
          )
        ),
        fill = backgroundColor,
        stroke = 0,
        strokeWeight = 0f
      )
  }
