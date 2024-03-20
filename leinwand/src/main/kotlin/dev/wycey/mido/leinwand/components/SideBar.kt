package dev.wycey.mido.leinwand.components

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.layout.Size

class SideBar(
  private val instanceId: Int,
  private val initialSize: Size = Size(300f, 702f),
  private val onChangeColor: (color: HSLuv) -> Unit
) :
  StatelessComponent("sideBar$instanceId") {
  override fun build(context: BuildContext) =
    VirtualBox(
      width = initialSize.width,
      height = initialSize.height,
      child =
        Box(
          fill = 0xff2b2d30.toInt(),
          child =
            Padding(
              EdgeInsets.all(8f),
              VStack(
                listOf(
                  Text("Colors", Styles.title),
                  ColorPickerWrapper(instanceId, onChangeColor),
                  VirtualBox(height = 8f),
                  ColorSelector(instanceId),
                  VirtualBox(height = 16f),
                  Text("Layers", Styles.title),
                  VirtualBox(height = 8f),
                  LayerList(instanceId)
                )
              )
            )
        )
    )
}
