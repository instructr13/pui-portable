package dev.wycey.mido.pui.renderer.data

import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererData

class ZStackRendererData : ContainerRendererData<BoxRenderer>() {
  var top: Float? = null
  var bottom: Float? = null
  var left: Float? = null
  var right: Float? = null

  var width: Float? = null
  var height: Float? = null

  val isPositioned: Boolean
    get() = top != null || bottom != null || left != null || right != null || width != null || height != null

  override fun toString(): String =
    "ZStackRendererData(top=$top, bottom=$bottom, left=$left, right=$right, width=$width, height=$height)"
}
