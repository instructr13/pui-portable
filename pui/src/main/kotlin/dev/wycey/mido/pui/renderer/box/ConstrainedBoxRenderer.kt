package dev.wycey.mido.pui.renderer.box

import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints

class ConstrainedBoxRenderer(_additionalConstraints: BoxConstraints) : ProxyBoxRenderer() {
  var additionalConstraints: BoxConstraints = _additionalConstraints
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayout()
    }

  override fun performLayout() {
    val constraints = this.constraints as BoxConstraints

    if (child != null) {
      child!!.let {
        it.layout(additionalConstraints.enforce(constraints), true)

        size = it.size
      }

      return
    }

    size = additionalConstraints.enforce(constraints).constrain(Size.ZERO)
  }

  override fun computeDryLayout(constraints: BoxConstraints): Size =
    if (child != null) {
      child!!.getDryLayout(additionalConstraints.enforce(constraints))
    } else {
      additionalConstraints.enforce(constraints).constrain(Size.ZERO)
    }
}
