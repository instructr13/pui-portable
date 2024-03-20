package dev.wycey.mido.pui.examples.colors

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import dev.wycey.mido.pui.util.processing.RenderMode
import processing.core.PGraphics

internal class HSLuvColorPickerBoxRenderer(
  initialCoordinate: Point,
  initialHue: Float,
  initialPickedColorCircleRadius: Float
) : BoxRenderer() {
  companion object {
    private fun paintPiece(
      g: PGraphics,
      coordinate: Point,
      h: Float,
      s: Float,
      l: Float
    ) {
      val rawColor = HSLuv(h, s, l).toSRGB().toRGBInt()
      val color = rawColor.argb.toInt()

      g.stroke(color)

      g.point(coordinate.x, coordinate.y)
    }
  }

  override fun computeDryLayout(constraints: BoxConstraints) =
    if (constraints.isFinite()) {
      constraints.biggest
    } else {
      constraints.smallest
    }

  private var previousCoordinate: Point = initialCoordinate

  var coordinate: Point = initialCoordinate
    set(value) {
      if (field == value) return

      previousCoordinate = field
      field = value

      needsRepaintCircle = true
    }

  var hue: Float = initialHue
    set(value) {
      if (field == value) return

      field = value

      needsRepaintPicker = true
    }

  private var previousRadius = initialPickedColorCircleRadius

  var pickedColorCircleRadius = initialPickedColorCircleRadius
    set(value) {
      if (field == value) return

      previousRadius = field
      field = value

      needsRepaintCircle = true
    }

  private var needsRepaintPicker = true
  private var needsRepaintCircle = true
  private var pickerGraphics: PGraphics? = null

  override fun performLayout() {
    size = getConstraints().biggest
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    if (needsRepaintPicker) {
      needsRepaintPicker = false

      pickerGraphics = d.applet.createGraphics(size.width.toInt(), size.height.toInt())

      val maxX = size.width.toDouble()
      val maxY = size.height.toDouble()

      pickerGraphics!!.noSmooth()
      pickerGraphics!!.beginDraw()
      pickerGraphics!!.strokeWeight(1f)

      for (x in 0..maxX.toInt()) {
        val s = ((x / maxX) * 100).coerceIn(0.0..100.0)

        for (y in 0..maxY.toInt()) {
          val l = 100 - ((y / maxY) * 100).coerceIn(0.0..100.0)

          paintPiece(pickerGraphics!!, Point(x, y), hue, s.toFloat(), l.toFloat())
        }
      }

      pickerGraphics!!.endDraw()
    }

    pickerGraphics?.let { d.image(it) }

    d.with(
      stroke = 0xff0000000.toInt(),
      strokeWeight = 1f,
      fill = 0x00ffffff,
      ellipseMode = RenderMode.Radius
    ) {
      d.with(stroke = 0xffffffff.toInt()) {
        d.ellipse(coordinate, Size(pickedColorCircleRadius - 1f, pickedColorCircleRadius - 1f))
      }

      d.ellipse(coordinate, Size(pickedColorCircleRadius, pickedColorCircleRadius))
    }
  }
}
