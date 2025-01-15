package dev.wycey.mido.pui.examples.colors

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.state.signals.untracked

public class HSLuvColorPicker
  @JvmOverloads
  constructor(
    private val onSetColor: (color: HSLuv) -> Unit,
    private val initialColor: HSLuv = HSLuv(0.0, 0.0, 0.0),
    private val pickerBoxSize: Int = 200,
    private val pickedColorCircleRadius: Float = 6f,
    private val hueBarWidth: Float = 20f,
    private var enableAlphaEdit: Boolean = false,
    key: String? = null
  ) : StatefulComponent(key) {
    override fun build(context: BuildContext): Component {
      var h by signal(initialColor.h)
      var s by signal(initialColor.s)
      var l by signal(initialColor.l)
      var alpha by signal(initialColor.alpha)

      return Padding(
        EdgeInsets.all(pickedColorCircleRadius),
        HStack(
          {
            val list = mutableListOf<Component>()

            list.addAll(
              listOf(
                HSLuvColorPickerBox(
                  { newS, newL ->
                    s = newS
                    l = newL

                    onSetColor(HSLuv(untracked { h }, newS, newL, untracked { alpha }))
                  },
                  HSLuv(h, s, l),
                  pickerBoxSize,
                  h,
                  pickedColorCircleRadius,
                  "pickerBox$h"
                ),
                VirtualBox(width = 8f),
                HSLuvHueBar(
                  {
                    h = it

                    onSetColor(HSLuv(it, untracked { s }, untracked { l }, untracked { alpha }))
                  },
                  h,
                  s,
                  l,
                  Size(hueBarWidth, pickerBoxSize.toFloat()),
                  "hueBar$s$l"
                )
              )
            )

            if (enableAlphaEdit) {
              list.addAll(
                listOf(
                  VirtualBox(width = 8f),
                  HSLuvAlphaBar(
                    {
                      alpha = it

                      onSetColor(HSLuv(untracked { h }, untracked { s }, untracked { l }, it))
                    },
                    alpha,
                    h,
                    s,
                    l,
                    Size(hueBarWidth, pickerBoxSize.toFloat()),
                    "alphaBar$h$s$l"
                  )
                )
              )
            }

            list
          }
        )
      )
    }
  }
