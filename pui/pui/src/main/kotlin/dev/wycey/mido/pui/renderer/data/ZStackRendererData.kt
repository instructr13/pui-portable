package dev.wycey.mido.pui.renderer.data

import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererData

public class ZStackRendererData : ContainerRendererData<BoxRenderer>() {
  public var top: Float? = null
  public var bottom: Float? = null
  public var left: Float? = null
  public var right: Float? = null

  public var width: Float? = null
  public var height: Float? = null

  public val isPositioned: Boolean
    get() = top != null || bottom != null || left != null || right != null || width != null || height != null

  override fun toString(): String =
    "ZStackRendererData(top=$top, bottom=$bottom, left=$left, right=$right, width=$width, height=$height)"
}
