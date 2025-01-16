package dev.wycey.mido.pui.renderer.box

import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.RenderGlobalContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.RendererVisitor
import dev.wycey.mido.pui.renderer.data.BoxRendererData
import dev.wycey.mido.pui.renderer.delegations.RendererWithChild
import dev.wycey.mido.pui.renderer.delegations.RendererWithChildContract
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

public abstract class ProxyBoxRenderer(
  private val rendererWithChild: RendererWithChild<BoxRenderer> = RendererWithChild()
) : BoxRenderer(),
  RendererWithChildContract<BoxRenderer> by rendererWithChild {
  init {
    rendererWithChild.that = this
  }

  override fun setupParentRendererData(child: RendererObject) {
    if (child.parentRendererData !is BoxRendererData) {
      child.parentRendererData = BoxRendererData()
    }
  }

  override fun computeDryLayout(constraints: BoxConstraints): Size =
    child?.getDryLayout(constraints) ?: constraints.smallest

  override fun performLayout() {
    size = child?.apply { layout(getConstraints(), true) }?.size ?: getConstraints().smallest
  }

  override fun attach(context: RenderGlobalContext) {
    super.attach(context)

    rendererWithChild.attach(context)
  }

  override fun detach() {
    super.detach()

    rendererWithChild.detach()
  }

  override fun redepthChildren() {
    super.redepthChildren()

    rendererWithChild.redepthChildren()
  }

  override fun visitChildren(visitor: RendererVisitor) {
    super.visitChildren(visitor)

    rendererWithChild.visitChildren(visitor)
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    super.paint(d, currentScope)

    child?.tryPaint(d, currentScope)
  }
}
