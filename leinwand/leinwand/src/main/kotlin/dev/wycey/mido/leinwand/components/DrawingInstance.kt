package dev.wycey.mido.leinwand.components

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.layout.Expanded
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.elements.base.BuildContext
import processing.core.PApplet

class DrawingInstance
  @JvmOverloads
  constructor(
    applet: PApplet,
    private val instanceId: Int,
    private val onChangeColor: (color: HSLuv) -> Unit = {}
  ) : StatelessComponent("drawing$instanceId") {
    init {
      DrawingRoot.applet = applet
      dev.wycey.mido.leinwand.LeinwandHandle.instances.computeIfAbsent(instanceId) {
        dev.wycey.mido.leinwand.LeinwandHandle(applet)
      }
    }

    override fun build(context: BuildContext) =
      Box(
        fill = 0xff43454a.toInt(),
        child =
          VStack(
            listOf(
              HStack(
                listOf(
                  Expanded(
                    VStack(
                      listOf(
                        CommandBar(instanceId),
                        BrushSelector(instanceId),
                        HStack(
                          listOf(
                            ToolSideBar(instanceId),
                            Canvas(instanceId),
                            VirtualBox()
                          ),
                          mainAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackMainAxisAlignment.SpaceBetween
                        )
                      )
                    )
                  ),
                  SideBar(instanceId, onChangeColor = onChangeColor)
                )
              ),
              StatusBar(instanceId)
            ),
            crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Stretch
          )
      )
  }
