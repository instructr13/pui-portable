package dev.wycey.mido.pui.renderer

import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import java.util.*

public open class RenderGlobalContext(
  scope: Scope? = null
) {
  public var nodesNeedingLayout: PriorityQueue<RendererObject> =
    PriorityQueue<RendererObject> { a, b -> a.depth - b.depth }
    private set

  public var nodesNeedingPaint: PriorityQueue<RendererObject> =
    PriorityQueue<RendererObject> { a, b -> a.depth - b.depth } // Currently only persistent root renderer
    private set

  public open var rootNode: RendererObject? = null
    set(value) {
      if (field == value) {
        return
      }

      field?.detach()
      field = value
      field?.attach(this)
    }

  public var scope: Scope? = null
    private set

  private var shouldMergeDirtyNodes = false
  private val children = mutableSetOf<RenderGlobalContext>()

  init {
    if (scope != null) {
      this.scope = scope
    }
  }

  public fun attach(scope: Scope) {
    this.scope = scope

    for (child in children) {
      child.attach(scope)
    }
  }

  public fun detach() {
    this.scope = null

    for (child in children) {
      child.detach()
    }
  }

  public fun adoptChild(child: RenderGlobalContext) {
    children.add(child)

    if (scope != null) {
      child.attach(scope!!)
    }
  }

  public fun dropChild(child: RenderGlobalContext) {
    children.remove(child)

    if (scope != null) {
      child.detach()
    }
  }

  public fun flushLayout() {
    try {
      while (nodesNeedingLayout.isNotEmpty()) {
        val dirtyNodes = nodesNeedingLayout.toList()

        nodesNeedingLayout.clear()

        for ((i, node) in dirtyNodes.withIndex()) {
          if (shouldMergeDirtyNodes) {
            shouldMergeDirtyNodes = false

            if (nodesNeedingLayout.isNotEmpty()) {
              nodesNeedingLayout.addAll(dirtyNodes.subList(i, dirtyNodes.size))

              break
            }
          }
          if (node.needsLayout && node.context == this) {
            node.layoutWithoutResize()
          }
        }

        shouldMergeDirtyNodes = false
      }

      for (child in children) {
        child.flushLayout()
      }
    } finally {
      shouldMergeDirtyNodes = false
    }
  }

  public fun flushPaint() {
    while (nodesNeedingPaint.isNotEmpty()) {
      val node = nodesNeedingPaint.poll()

      if (node.context == this) {
        node.paint(AppletDrawer(scope!!.applet), scope!!)
      }
    }

    for (child in children) {
      child.flushPaint()
    }
  }
}
