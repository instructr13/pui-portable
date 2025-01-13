package dev.wycey.mido.pui.renderer.box

import dev.wycey.mido.pui.layout.Alignment
import dev.wycey.mido.pui.layout.AlignmentFactor
import dev.wycey.mido.pui.renderer.data.BoxRendererData

open class AlignedBoxRenderer(initialAlignment: AlignmentFactor? = Alignment.center, child: BoxRenderer? = null) :
  ShiftedBoxRenderer(child) {
  var alignment: AlignmentFactor? = initialAlignment
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayout()
    }

  protected fun alignChild() {
    val childParentRendererData = child!!.parentRendererData as BoxRendererData

    childParentRendererData.offset = alignment!!.alongOffset((size - child!!.size).toPoint())
  }
}
