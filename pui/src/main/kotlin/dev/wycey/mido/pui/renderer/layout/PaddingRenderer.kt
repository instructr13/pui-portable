package dev.wycey.mido.pui.renderer.layout

import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.box.ShiftedBoxRenderer
import dev.wycey.mido.pui.renderer.data.BoxRendererData

class PaddingRenderer(preferredPadding: EdgeInsets, child: BoxRenderer? = null) : ShiftedBoxRenderer(child) {
  var padding = preferredPadding
    set(value) {
      field = value

      markNeedsLayout()
    }

  override fun computeDryLayout(constraints: BoxConstraints): Size {
    if (child == null) {
      return constraints.constrain(
        Size(
          padding.left + padding.right,
          padding.top + padding.bottom
        )
      )
    }

    val innerConstraints = constraints.deflate(padding)
    val childSize = child!!.getDryLayout(innerConstraints)

    return constraints.constrain(
      Size(
        childSize.width + padding.left + padding.right,
        childSize.height + padding.top + padding.bottom
      )
    )
  }

  override fun performLayout() {
    val constraints = getConstraints()

    if (child == null) {
      size =
        constraints.constrain(
          Size(
            padding.left + padding.right,
            padding.top + padding.bottom
          )
        )

      return
    }

    val innerConstraints = constraints.deflate(padding)

    child!!.layout(innerConstraints, true)

    val childParentRendererData = child!!.parentRendererData as BoxRendererData

    childParentRendererData.offset = Point(padding.left, padding.top)

    size =
      constraints.constrain(
        Size(
          child!!.size.width + padding.left + padding.right,
          child!!.size.height + padding.top + padding.bottom
        )
      )
  }
}
