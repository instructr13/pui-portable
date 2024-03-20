package dev.wycey.mido.leinwand.layers

import dev.wycey.mido.leinwand.LeinwandHandle
import dev.wycey.mido.leinwand.draw.Drawable
import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PImage
import java.util.*

internal open class Layer(applet: PApplet, var name: String, val handle: LeinwandHandle) :
  LayerContract, Drawable {
  val id: String = UUID.randomUUID().toString()
  var hidden = false
  var lock = false

  var reference = false
  var blendMode: Int = PApplet.NORMAL

  final override val g: PGraphics =
    handle.canvasSize.let {
      applet.createGraphics(it.width.toInt(), it.height.toInt())
    }

  init {
    g.beginDraw()
    g.background(255, 0f)
    g.endDraw()

    commit()
  }

  internal lateinit var thumbnail: PImage
    private set

  fun commit() {
    val image = g.get()

    image.resize(40, 40)

    thumbnail = image
  }

  override fun draw(base: PGraphics) {
    if (hidden) return

    base.blendMode(blendMode)
    base.image(g, 0f, 0f)
  }

  override fun toString() = "Layer(name='$name', id='$id', hidden=$hidden, lock=$lock, reference=$reference)"
}
