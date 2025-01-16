package dev.wycey.mido.pui.renderer.layout

import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.box.ProxyBoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

public class OffStageRenderer(
  initialOffstage: Boolean = true,
  child: BoxRenderer? = null
) : ProxyBoxRenderer() {
  init {
    super.child = child
  }

  public var offstage: Boolean = initialOffstage
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayoutForSizedByParentChange()
    }

  override fun computeDryLayout(constraints: BoxConstraints): Size {
    if (offstage) return constraints.smallest

    return super.computeDryLayout(constraints)
  }

  override fun performResize() {
    assert(offstage)

    super.performResize()
  }

  override fun performLayout() {
    if (offstage) {
      size = child?.apply { layout(getConstraints()) }?.size ?: getConstraints().smallest
    } else {
      super.performLayout()
    }
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    if (offstage) return

    super.paint(d, currentScope)
  }
}
