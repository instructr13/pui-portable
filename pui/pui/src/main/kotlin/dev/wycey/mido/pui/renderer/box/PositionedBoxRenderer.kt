package dev.wycey.mido.pui.renderer.box

import dev.wycey.mido.pui.layout.Alignment
import dev.wycey.mido.pui.layout.AlignmentFactor
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints

public class PositionedBoxRenderer(initialAlignment: AlignmentFactor = Alignment.center, child: BoxRenderer? = null) :
  AlignedBoxRenderer(initialAlignment, child) {
  override fun computeDryLayout(constraints: BoxConstraints): Size {
    val shrinkWrapWidth = constraints.maxWidth == Float.POSITIVE_INFINITY
    val shrinkWrapHeight = constraints.maxHeight == Float.POSITIVE_INFINITY

    if (child != null) {
      val childSize = child!!.getDryLayout(constraints.loosen())

      return Size(
        if (shrinkWrapWidth) childSize.width else constraints.maxWidth,
        if (shrinkWrapHeight) childSize.height else constraints.maxHeight
      )
    }

    return constraints.constrain(
      Size(
        if (shrinkWrapWidth) 0f else Float.POSITIVE_INFINITY,
        if (shrinkWrapHeight) 0f else Float.POSITIVE_INFINITY
      )
    )
  }

  override fun performLayout() {
    val constraints = getConstraints()
    val shrinkWrapWidth = constraints.maxWidth == Float.POSITIVE_INFINITY
    val shrinkWrapHeight = constraints.maxHeight == Float.POSITIVE_INFINITY

    if (child != null) {
      child!!.layout(constraints.loosen(), true)

      size =
        constraints.constrain(
          Size(
            if (shrinkWrapWidth) child!!.size.width else constraints.maxWidth,
            if (shrinkWrapHeight) child!!.size.height else constraints.maxHeight
          )
        )

      alignChild()

      return
    }

    size =
      constraints.constrain(
        Size(
          if (shrinkWrapWidth) 0f else Float.POSITIVE_INFINITY,
          if (shrinkWrapHeight) 0f else Float.POSITIVE_INFINITY
        )
      )
  }
}
