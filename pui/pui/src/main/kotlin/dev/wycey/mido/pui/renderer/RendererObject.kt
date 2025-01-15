package dev.wycey.mido.pui.renderer

import dev.wycey.mido.pui.layout.constraints.Constraints
import dev.wycey.mido.pui.renderer.data.ParentRendererData
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

internal typealias RendererVisitor = (child: RendererObject) -> Unit

public abstract class RendererObject {
  public var parent: RendererObject? = null
    private set

  public var context: RenderGlobalContext? = null
    private set

  public var parentRendererData: ParentRendererData? = null

  public var depth: Int = 0
    private set

  public open var constraints: Constraints? = null

  public open val attached: Boolean
    get() = context != null

  protected open val sizedByParent: Boolean = false

  public var needsLayout: Boolean = true

  private var relayoutBoundary: RendererObject? = null

  public open fun setupParentRendererData(child: RendererObject) {
    if (child.parentRendererData !is ParentRendererData) {
      child.parentRendererData = ParentRendererData()
    }
  }

  public open fun redepthChild(child: RendererObject) {
    if (child.depth <= depth) {
      child.depth = depth + 1
      child.redepthChildren()
    }
  }

  protected open fun redepthChildren() {}

  public open fun insertChild(child: RendererObject) {
    setupParentRendererData(child)
    markNeedsLayout()

    child.parent = this

    if (attached) {
      child.attach(context!!)
    }

    redepthChild(child)
    child.performInsertChild()
  }

  public open fun performInsertChild() {}

  public open fun dropChild(child: RendererObject) {
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

  public open fun performDrop() {}

  public fun markParentNeedsLayout() {
    needsLayout = true

    parent!!.markNeedsLayout()
  }

  public open fun markNeedsLayout() {
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

  public open fun markNeedsLayoutForSizedByParentChange() {
    markNeedsLayout()
    markParentNeedsLayout()
  }

  protected open fun performResize() {}

  protected open fun performLayout() {}

  public open fun layout(
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

  public open fun layoutWithoutResize() {
    performLayout()

    needsLayout = false
  }

  public fun scheduleInitialLayout() {
    relayoutBoundary = this

    context!!.nodesNeedingLayout.add(this)
    context!!.nodesNeedingPaint.add(this)
  }

  public open fun attach(context: RenderGlobalContext) {
    this.context = context

    if (needsLayout && relayoutBoundary != null) {
      needsLayout = false

      markNeedsLayout()
    }
  }

  public open fun detach() {
    context = null
  }

  public open fun visitChildren(visitor: RendererVisitor) {}

  public open fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
  }

  public var firstLayout: Boolean = false
    private set

  public fun tryPaint(
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
