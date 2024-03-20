package dev.wycey.mido.pui.components.output

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.layout.Center
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.layout.ZStack
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.renderer.BorderRadius

class ProgressBar
  @JvmOverloads
  constructor(
    var value: Float = 0f,
    private val min: Float = 0f,
    private val max: Float = 100f,
    private val barSize: Float = 200f,
    private val barThickness: Float = 4f,
    private val barColor: Int = 0xFFDFDFDF.toInt(),
    private val trackColor: Int = 0xFF5886E0.toInt(),
    key: String? = null
  ) : StatelessComponent(key) {
    override fun build(context: BuildContext): Component {
      val bar =
        VirtualBox(
          Box(
            borderRadius = BorderRadius.all(5f),
            fill = barColor
          ),
          width = barSize,
          height = barThickness
        )

      val trackSize = value / (max - min) * barSize

      val track =
        VirtualBox(
          Box(
            borderRadius = BorderRadius.all(5f),
            fill = trackColor
          ),
          width = trackSize,
          height = barThickness
        )

      return VirtualBox(
        ZStack(
          listOf(
            Center(
              bar
            ),
            Center(
              Padding(
                EdgeInsets.only(
                  right = barSize - trackSize
                ),
                track
              )
            )
          )
        ),
        width = barSize,
        height = barThickness
      )
    }
  }
