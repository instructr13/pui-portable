package dev.wycey.mido.pui.renderer.delegations

import dev.wycey.mido.pui.renderer.RenderGlobalContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.RendererVisitor
import dev.wycey.mido.pui.util.hasSuperclassUntil

internal interface RendererWithChildContract<ChildType : RendererObject> {
  var child: ChildType?

  fun attach(context: RenderGlobalContext)

  fun detach()

  fun redepthChildren()

  fun visitChildren(visitor: RendererVisitor)
}

public open class RendererWithChild<ChildType : RendererObject> : RendererWithChildContract<ChildType>,
  RendererObject() {
  internal lateinit var that: RendererObject

  private val safeThat: RendererObject?
    get() = if (this::that.isInitialized && that != this) that else null

  override var child: ChildType? = null
    set(value) {
      if (field != null) {
        that.dropChild(field!!)
      }

      field = value

      if (field != null) {
        that.insertChild(field!!)
      }
    }

  override fun attach(context: RenderGlobalContext) {
    if (that::class.hasSuperclassUntil(RendererWithChild::class)) {
      super.attach(context)
    }

    child?.attach(context)
  }

  override fun detach() {
    if (that::class.hasSuperclassUntil(RendererWithChild::class)) {
      super.detach()
    }

    child?.detach()
  }

  override fun redepthChildren() {
    if (that::class.hasSuperclassUntil(RendererWithChild::class)) {
      super.redepthChildren()
    }

    if (child != null) {
      safeThat?.redepthChild(child!!)
    }
  }

  override fun visitChildren(visitor: RendererVisitor) {
    if (that::class.hasSuperclassUntil(RendererWithChild::class)) {
      super.visitChildren(visitor)
    }

    if (child != null) {
      visitor(child!!)
    }
  }
}
