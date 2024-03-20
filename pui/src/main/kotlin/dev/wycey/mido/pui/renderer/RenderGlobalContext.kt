package dev.wycey.mido.pui.renderer

import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import java.util.*

open class RenderGlobalContext(scope: Scope? = null) {
  var nodesNeedingLayout = PriorityQueue<RendererObject> { a, b -> a.depth - b.depth }
    private set

  var nodesNeedingPaint =
    PriorityQueue<RendererObject> { a, b -> a.depth - b.depth } // Currently only persistent root renderer
    private set

  open var rootNode: RendererObject? = null
    set(value) {
      if (field == value) {
        return
      }

      field?.detach()
      field = value
      field?.attach(this)
    }

  var scope: Scope? = null

  private var shouldMergeDirtyNodes = false
  private val children = mutableSetOf<RenderGlobalContext>()

  init {
    if (scope != null) {
      this.scope = scope
    }
  }

  fun attach(scope: Scope) {
    this.scope = scope

    for (child in children) {
      child.attach(scope)
    }
  }

  fun detach() {
    this.scope = null

    for (child in children) {
      child.detach()
    }
  }

  fun adoptChild(child: RenderGlobalContext) {
    children.add(child)

    if (scope != null) {
      child.attach(scope!!)
    }
  }

  fun dropChild(child: RenderGlobalContext) {
    children.remove(child)

    if (scope != null) {
      child.detach()
    }
  }

  fun flushLayout() {
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

  fun flushPaint() {
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
