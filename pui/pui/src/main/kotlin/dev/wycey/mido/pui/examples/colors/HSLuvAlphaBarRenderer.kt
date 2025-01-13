package dev.wycey.mido.pui.examples.colors

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import processing.core.PGraphics

internal class HSLuvAlphaBarRenderer(
  var h: Float,
  var s: Float,
  var l: Float,
  initialAlpha: Float
) : BoxRenderer() {
  companion object {
    private fun paintAlpha(
      g: PGraphics,
      currentScope: Scope,
      h: Float,
      s: Float,
      l: Float,
      size: Size
    ) {
      val maxY = size.height

      for (y in 0..maxY.toInt()) {
        val alpha = (y / maxY).coerceIn(0f..1f)

        val rawColor = HSLuv(h, s, l).toSRGB().toRGBInt()
        val color = rawColor.argb.toInt()

        g.stroke(color, alpha * 255)
        g.line(0f, y.toFloat(), size.width, y.toFloat())
      }
    }
  }

  var alpha: Float = initialAlpha
    set(value) {
      if (field == value) return

      field = value

      needsRepaintAlphaBar = true
    }

  private var needsRepaintAlphaBar = true
  private lateinit var alphaBarBaseGraphics: PGraphics
  private var alphaBarOverlayGraphics: PGraphics? = null

  override fun computeDryLayout(constraints: BoxConstraints) =
    if (constraints.isFinite()) {
      constraints.biggest
    } else {
      constraints.smallest
    }

  override fun performLayout() {
    size = getConstraints().biggest
  }

  private val alphaBoxSize = 10f
  private val alphaGreyBoxColor = 0xff808080.toInt()
  private val alphaTransparentBoxColor = 0x00ffffff

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    if (!this::alphaBarBaseGraphics.isInitialized) {
      alphaBarBaseGraphics = d.applet.createGraphics(size.width.toInt(), size.height.toInt())

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

    d.with(fill = 0xffffffff.toInt()) {
      d.applet.noStroke()
      d.rect(Point.ZERO, size)
    }

    d.image(alphaBarBaseGraphics)

    if (needsRepaintAlphaBar) {
      needsRepaintAlphaBar = false

      alphaBarOverlayGraphics = d.applet.createGraphics(size.width.toInt(), size.height.toInt())

      alphaBarOverlayGraphics!!.noSmooth()
      alphaBarOverlayGraphics!!.beginDraw()
      alphaBarOverlayGraphics!!.clear()

      paintAlpha(alphaBarOverlayGraphics!!, currentScope, h, s, l, size)

      alphaBarOverlayGraphics!!.endDraw()
    }

    d.image(alphaBarOverlayGraphics!!)

    d.with(stroke = 0xff000000.toInt(), strokeWeight = 1f) {
      d.line(Point(0f, alpha * size.height), Point(size.width - 1f, alpha * size.height))
    }
  }
}
