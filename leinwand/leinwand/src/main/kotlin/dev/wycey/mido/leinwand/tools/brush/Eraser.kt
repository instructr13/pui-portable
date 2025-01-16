package dev.wycey.mido.leinwand.tools.brush

import dev.wycey.mido.leinwand.components.DrawingRoot.applet
import dev.wycey.mido.leinwand.draw.Draggable
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventType
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.state.signals.untracked
import processing.core.PGraphics
import processing.core.PImage
import kotlin.math.sqrt

internal class Eraser(
  handle: dev.wycey.mido.leinwand.LeinwandHandle
) : Brush(handle),
  Draggable {
  override val name: String
    get() = "Eraser"

  override var startingSize: Int = 1
  override var size: Int = startingSize

  override fun createCursor(size: Int): Pair<PImage, Point> {
    val actualSize = size + 4
    val cursorGraphics = applet.createGraphics(actualSize, actualSize)

    cursorGraphics.noSmooth()
    cursorGraphics.beginDraw()

    cursorGraphics.fill(0x00ffffff, 0f)
    cursorGraphics.stroke(0)
    cursorGraphics.rect(1f, 1f, actualSize.toFloat() - 3, actualSize.toFloat() - 3)
    cursorGraphics.stroke(0xffffffff.toInt())
    cursorGraphics.rect(0f, 0f, actualSize.toFloat() - 1, actualSize.toFloat() - 1)

    cursorGraphics.endDraw()

    return Pair(cursorGraphics.get(), Point(actualSize / 2f, actualSize / 2f))
  }

  private var pressed = false

  override fun onPress(
    e: GestureEventArgs,
    type: GestureEventType.Press
  ) {
    pressed = true
  }

  private var prevX = -1f
  private var prevY = -1f

  fun relationalStrokeInterpolation(
    g: PGraphics,
    desiredImage: PImage,
    current: Point,
    prev: Point
  ) {
    val lv = current - prev
    val accuracy = 1 / sqrt(lv.x * lv.x + lv.y * lv.y)

    generateSequence(0.0f) { it + accuracy }.takeWhile { it < 1.0f }.forEach {
      val p = prev + Point(lv.x * it, lv.y * it)

      g.image(desiredImage, p.x - size / 2, p.y - size / 2)
    }
  }

  private val brushImageColorMap = mutableMapOf<Int, PImage>()

  override fun onDrag(
    e: GestureEventArgs,
    type: GestureEventType.Drag
  ) {
    val g = currentLayer.g

    if (prevX == -1f && prevY == -1f) {
      prevX = e.delta.first
      prevY = e.delta.second
    }

    g.beginDraw()

    val color =
      untracked { handle.backgroundColor }
        .toSRGB()
        .toRGBInt()
        .argb
        .toInt()

    val image =
      brushImageColorMap.getOrPut(color) {
        val brushImage = applet.createGraphics(size, size)

        brushImage.beginDraw()
        brushImage.fill(color)
        brushImage.noStroke()
        brushImage.rect(0f, 0f, size.toFloat(), size.toFloat())
        brushImage.endDraw()

        brushImage.get()
      }

    image.resize(size, size)

    relationalStrokeInterpolation(g, image, Point(e.delta.first, e.delta.second), Point(prevX, prevY))

    g.endDraw()

    prevX = e.delta.first
    prevY = e.delta.second
  }

  override fun onRelease(
    e: GestureEventArgs,
    type: GestureEventType.Release
  ) {
    pressed = false

    currentLayer.commit()

    prevX = -1f
    prevY = -1f
  }
}
