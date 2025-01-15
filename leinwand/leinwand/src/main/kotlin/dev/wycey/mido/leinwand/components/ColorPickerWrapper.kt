package dev.wycey.mido.leinwand.components

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.examples.colors.HSLuvColorPicker
import dev.wycey.mido.pui.state.signals.untracked

internal class ColorPickerWrapper(
  private val instanceId: Int,
  private val onChangeColor: (color: HSLuv) -> Unit
) : StatefulComponent("colorPickerWrapper$instanceId") {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!
    val color = if (handle.selectingForeground) handle.foregroundColor else handle.backgroundColor

    effect {
      // subscribe both
      handle.foregroundColor
      handle.backgroundColor

      if (handle.selectingForeground) {
        onChangeColor(handle.foregroundColor)
      } else {
        onChangeColor(handle.backgroundColor)
      }
    }

    return HSLuvColorPicker(
      {
        if (untracked { handle.selectingForeground }) {
          handle.foregroundColor = it
        } else {
          handle.backgroundColor = it
        }
      },
      color,
      enableAlphaEdit = true,
      key = "colorPicker${handle.selectingForeground}"
    )
  }
}
