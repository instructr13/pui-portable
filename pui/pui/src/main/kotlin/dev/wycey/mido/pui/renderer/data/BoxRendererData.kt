package dev.wycey.mido.pui.renderer.data

import dev.wycey.mido.pui.layout.Point

public open class BoxRendererData(
  internal var offset: Point = Point.ZERO
) : ParentRendererData()
