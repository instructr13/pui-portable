package dev.wycey.mido.leinwand.components

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.ZStack
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.renderer.layout.ZStackFit
import processing.core.PGraphics

internal class ColorPalette(private val color: HSLuv) : StatelessComponent() {
  private lateinit var alphaBarBaseGraphics: PGraphics
  private val alphaBoxSize = 4f
  private val alphaGreyBoxColor = 0xff808080.toInt()
  private val alphaTransparentBoxColor = 0x00ffffff

  override fun build(context: BuildContext) =
    Box(
      stroke = 0xff000000.toInt(),
      child =
        Padding(
          EdgeInsets.only(1f, 1f, 1f, 2f),
          ZStack(
            fit = ZStackFit.Expand,
            children =
              listOf(
                Box(
                  additionalPaint = { d, _, size ->
                    if (!this::alphaBarBaseGraphics.isInitialized) {
                      alphaBarBaseGraphics = d.applet.createGraphics(size.width.toInt(), size.height.toInt() - 1)

                      alphaBarBaseGraphics.noSmooth()
                      alphaBarBaseGraphics.beginDraw()
                      alphaBarBaseGraphics.noStroke()

                      for (y in 0..(size.height / alphaBoxSize).toInt()) {
                        for (x in 0..(size.width / alphaBoxSize).toInt()) {
                          val color = if ((x + y) % 2 == 0) alphaGreyBoxColor else alphaTransparentBoxColor

                          alphaBarBaseGraphics.fill(color)
                          alphaBarBaseGraphics.rect(x * alphaBoxSize, y * alphaBoxSize, alphaBoxSize, alphaBoxSize)
                        }
                      }

                      alphaBarBaseGraphics.endDraw()
                    }

                    d.image(alphaBarBaseGraphics)
                  }
                ),
                Box(
                  fill = color.toSRGB().toRGBInt().argb.toInt()
                )
              )
          )
        )
    )
}
