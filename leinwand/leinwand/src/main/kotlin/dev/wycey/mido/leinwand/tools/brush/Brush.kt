package dev.wycey.mido.leinwand.tools.brush

import dev.wycey.mido.leinwand.tools.Tool
import dev.wycey.mido.pui.layout.Point
import processing.core.PApplet
import processing.core.PImage

internal abstract class Brush(protected val handle: dev.wycey.mido.leinwand.LeinwandHandle) : Tool {
  internal val currentLayer get() = handle.layers[handle.activeLayerIndex]

  abstract var startingSize: Int
  abstract var size: Int
  protected val imageCache = mutableMapOf<Int, Pair<PImage, Point>>()

  fun defaultCreateCursor(size: Int) = imageCache.getOrPut(size) { createCursor(size) }

  abstract fun createCursor(size: Int): Pair<PImage, Point>

  override fun applyCursor(applet: PApplet) {
    val (image, hotspot) = defaultCreateCursor(size)

    applet.cursor(image, hotspot.x.toInt(), hotspot.y.toInt())
  }
}
