package dev.wycey.mido.pui.renderer.box

import dev.wycey.mido.pui.renderer.data.BoxRendererData
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

public open class ShiftedBoxRenderer(child: BoxRenderer? = null) : ProxyBoxRenderer() {
  init {
    super.child = child
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    val child = child ?: return

    val childParentRendererData = child.parentRendererData!! as BoxRendererData

    currentScope.nestPositionalScope(childParentRendererData.offset) {
      child.paint(d, it)
    }
  }
}
