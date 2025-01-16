package dev.wycey.mido.leinwand.tools.brush

import dev.wycey.mido.leinwand.components.DrawingRoot.applet
import dev.wycey.mido.leinwand.draw.Draggable
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventType
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.state.signals.untracked
import processing.core.PApplet
import processing.core.PImage

internal class NormalBrush(
  handle: dev.wycey.mido.leinwand.LeinwandHandle
) : Brush(handle),
  Draggable {
  override val name: String
    get() = "Brush"

  override var startingSize: Int = 1
  override var size: Int = startingSize

  override fun createCursor(size: Int): Pair<PImage, Point> {
    val actualSize = size + 4
    val cursorGraphics = applet.createGraphics(actualSize, actualSize)

    cursorGraphics.noSmooth()
    cursorGraphics.beginDraw()

    cursorGraphics.fill(0x00ffffff, 0f)
    cursorGraphics.stroke(0)
    cursorGraphics.ellipseMode(PApplet.CORNER)
    cursorGraphics.ellipse(1f, 1f, actualSize.toFloat() - 3, actualSize.toFloat() - 3)
    cursorGraphics.stroke(0xffffffff.toInt())
    cursorGraphics.ellipse(0f, 0f, actualSize.toFloat() - 1, actualSize.toFloat() - 1)

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
    g.stroke(
      untracked { handle.foregroundColor }
        .toSRGB()
        .toRGBInt()
        .argb
        .toInt()
    )
    g.strokeWeight(size.toFloat())
    g.line(e.delta.first, e.delta.second, prevX, prevY)
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
