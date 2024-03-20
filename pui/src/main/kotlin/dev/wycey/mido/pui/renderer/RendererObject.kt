package dev.wycey.mido.pui.renderer

import dev.wycey.mido.pui.layout.constraints.Constraints
import dev.wycey.mido.pui.renderer.data.ParentRendererData
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

typealias RendererVisitor = (child: RendererObject) -> Unit

abstract class RendererObject {
  var parent: RendererObject? = null
    private set

  var context: RenderGlobalContext? = null
    private set

  var parentRendererData: ParentRendererData? = null

  var depth = 0
    private set

  open var constraints: Constraints? = null

  open val attached get() = context != null

  protected open val sizedByParent = false

  var needsLayout = true
  private var relayoutBoundary: RendererObject? = null

  open fun setupParentRendererData(child: RendererObject) {
    if (child.parentRendererData !is ParentRendererData) {
      child.parentRendererData = ParentRendererData()
    }
  }

  open fun redepthChild(child: RendererObject) {
    if (child.depth <= depth) {
      child.depth = depth + 1
      child.redepthChildren()
    }
  }

  protected open fun redepthChildren() {}

  open fun insertChild(child: RendererObject) {
    setupParentRendererData(child)
    markNeedsLayout()

    child.parent = this

    if (attached) {
      child.attach(context!!)
    }

    redepthChild(child)
    child.performInsertChild()
  }

  open fun performInsertChild() {}

  open fun dropChild(child: RendererObject) {
    child.performDrop()
    child.cleanRelayoutBoundary()
    child.parentRendererData!!.detach()
    child.parentRendererData = null
    child.parent = null

    if (attached) {
      child.detach()
    }

    markNeedsLayout()
  }

  open fun performDrop() {}

  fun markParentNeedsLayout() {
    needsLayout = true

    parent!!.markNeedsLayout()
  }

  open fun markNeedsLayout() {
    if (relayoutBoundary == null) {
      needsLayout = true

      if (parent != null) {
        markParentNeedsLayout()
      }

      return
    }

    if (relayoutBoundary != this) {
      markParentNeedsLayout()

      return
    }

    needsLayout = true

    context?.nodesNeedingLayout?.add(this)
  }

  open fun markNeedsLayoutForSizedByParentChange() {
    markNeedsLayout()
    markParentNeedsLayout()
  }

  protected open fun performResize() {
  }

  protected open fun performLayout() {
  }

  open fun layout(
    constraints: Constraints,
    parentUsesMySize: Boolean = false
  ) {
    val isRelayoutBoundary =
      !parentUsesMySize || sizedByParent || constraints.isTight || parent !is RendererObject

    val relayoutBoundary =
      if (isRelayoutBoundary) {
        this
      } else {
        parent!!.relayoutBoundary!!
      }

    if (!needsLayout && this.constraints == constraints) {
      if (relayoutBoundary != this.relayoutBoundary) {
        this.relayoutBoundary = relayoutBoundary

        visitChildren(::propagateRelayoutBoundaryToChild)
      }

      return
    }

    this.constraints = constraints

    if (this.relayoutBoundary != null && relayoutBoundary != this.relayoutBoundary) {
      visitChildren(::cleanChildRelayoutBoundary)
    }

    this.relayoutBoundary = relayoutBoundary

    if (sizedByParent) {
      performResize()
    }

    performLayout()

    needsLayout = false
  }

  open fun layoutWithoutResize() {
    performLayout()

    needsLayout = false
  }

  fun scheduleInitialLayout() {
    relayoutBoundary = this

    context!!.nodesNeedingLayout.add(this)
    context!!.nodesNeedingPaint.add(this)
  }

  open fun attach(context: RenderGlobalContext) {
    this.context = context

    if (needsLayout && relayoutBoundary != null) {
      needsLayout = false

      markNeedsLayout()
    }
  }

  open fun detach() {
    context = null
  }

  open fun visitChildren(visitor: RendererVisitor) {}

  open fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {}

  var firstLayout = false

  fun tryPaint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    if (needsLayout) {
      return
    }

    paint(d, currentScope)
  }

  private fun cleanRelayoutBoundary() {
    if (relayoutBoundary != this) {
      relayoutBoundary = null

      visitChildren(RendererObject::cleanRelayoutBoundary)
    }
  }

  private fun cleanChildRelayoutBoundary(child: RendererObject) {
    child.cleanRelayoutBoundary()
  }

  private fun propagateRelayoutBoundary() {
    if (relayoutBoundary == this) return

    val parentRelayoutBoundary = parent?.relayoutBoundary

    if (parentRelayoutBoundary != relayoutBoundary) {
      relayoutBoundary = parentRelayoutBoundary

      visitChildren(::propagateRelayoutBoundaryToChild)
    }
  }

  private fun propagateRelayoutBoundaryToChild(child: RendererObject) {
    child.propagateRelayoutBoundary()
  }
}
