package dev.wycey.mido.pui.examples.colors

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import processing.core.PGraphics

internal class HSLuvHueBarRenderer(
  var h: Float,
  initialS: Float,
  initialL: Float
) : BoxRenderer() {
  companion object {
    private fun paintHue(
      g: PGraphics,
      currentScope: Scope,
      s: Float,
      l: Float,
      size: Size
    ) {
      val maxY = size.height

      for (y in 0..maxY.toInt()) {
        val h = (y / maxY * 360f).coerceIn(0f..360f)

        val rawColor = HSLuv(h, s, l).toSRGB().toRGBInt()
        val color = rawColor.argb.toInt()

        g.stroke(color)
        g.line(0f, y.toFloat(), size.width, y.toFloat())
      }
    }
  }

  var s: Float = initialS
    set(value) {
      if (field == value) return

      field = value

      needsRepaintHueBar = true
    }

  var l: Float = initialL
    set(value) {
      if (field == value) return

      field = value

      needsRepaintHueBar = true
    }

  private var needsRepaintHueBar = true
  private var hueBarGraphics: PGraphics? = null

  override fun computeDryLayout(constraints: BoxConstraints) =
    if (constraints.isFinite()) {
      constraints.biggest
    } else {
      constraints.smallest
    }

  override fun performLayout() {
    size = getConstraints().biggest
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    if (needsRepaintHueBar) {
      needsRepaintHueBar = false

      hueBarGraphics = d.applet.createGraphics(size.width.toInt(), size.height.toInt())

      hueBarGraphics!!.noSmooth()
      hueBarGraphics!!.beginDraw()

      paintHue(hueBarGraphics!!, currentScope, s, l, size)

      hueBarGraphics!!.endDraw()
    }

    d.image(hueBarGraphics!!)

    d.with(stroke = 0xff000000.toInt(), strokeWeight = 1f) {
      d.line(Point(0f, h / 360f * size.height), Point(size.width - 1f, h / 360f * size.height))
    }
  }
}
