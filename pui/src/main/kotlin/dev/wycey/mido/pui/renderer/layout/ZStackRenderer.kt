package dev.wycey.mido.pui.renderer.layout

import dev.wycey.mido.pui.layout.*
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.RenderGlobalContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.data.ZStackRendererData
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererContract
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererData
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererImpl
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

enum class ZStackFit {
  Loose,
  Expand,
  PassThrough
}

open class ZStackRenderer(
  _alignment: AlignmentFactor = AlignmentDirectional.topStart,
  _fit: ZStackFit = ZStackFit.Loose,
  var ignoreVisualOverflow: Boolean = false,
  children: List<BoxRenderer> = emptyList(),
  private val containerRendererImpl: ContainerRendererImpl<BoxRenderer, ZStackRendererData> = ContainerRendererImpl()
) : BoxRenderer(),
  ContainerRendererContract<BoxRenderer> by containerRendererImpl {
  companion object {
    @JvmStatic
    fun layoutPositionedChild(
      child: BoxRenderer,
      childParentRendererData: ZStackRendererData,
      size: Size,
      alignment: Alignment
    ): Boolean {
      assert(childParentRendererData.isPositioned)
      assert(child.parentRendererData == childParentRendererData)

      var hasOverflow = false
      var childConstraints = BoxConstraints()

      if (childParentRendererData.left != null && childParentRendererData.right != null) {
        childConstraints =
          childConstraints.tighten(
            width = size.width - childParentRendererData.right!! - childParentRendererData.left!!
          )
      } else if (childParentRendererData.width != null) {
        childConstraints = childConstraints.tighten(width = childParentRendererData.width!!)
      }

      if (childParentRendererData.top != null && childParentRendererData.bottom != null) {
        childConstraints =
          childConstraints.tighten(
            height = size.height - childParentRendererData.bottom!! - childParentRendererData.top!!
          )
      } else if (childParentRendererData.height != null) {
        childConstraints = childConstraints.tighten(height = childParentRendererData.height!!)
      }

      child.layout(childConstraints, true)

      val x =
        when {
          childParentRendererData.left != null -> {
            childParentRendererData.left!!
          }

          childParentRendererData.right != null -> {
            size.width - childParentRendererData.right!! - child.size.width
          }

          else -> {
            alignment.alongOffset((size - child.size).toPoint()).x
          }
        }

      if (x < 0f || x + child.size.width > size.width) {
        hasOverflow = true
      }

      val y =
        when {
          childParentRendererData.top != null -> {
            childParentRendererData.top!!
          }

          childParentRendererData.bottom != null -> {
            size.height - childParentRendererData.bottom!! - child.size.height
          }

          else -> {
            alignment.alongOffset((size - child.size).toPoint()).y
          }
        }

      if (y < 0f || y + child.size.height > size.height) {
        hasOverflow = true
      }

      childParentRendererData.offset = Point(x, y)

      return hasOverflow
    }
  }

  init {
    containerRendererImpl.that = this

    addAll(children)
  }

  private var hasVisualOverflow = false

  var resolvedAlignment: Alignment? = null

  private fun resolve() {
    if (resolvedAlignment != null) return

    resolvedAlignment = alignment.resolve()
  }

  private fun markNeedsResolution() {
    resolvedAlignment = null

    markNeedsLayout()
  }

  var alignment: AlignmentFactor = _alignment
    set(value) {
      if (field == value) return

      field = value

      markNeedsResolution()
    }

  var fit: ZStackFit = _fit
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayout()
    }

  override fun setupParentRendererData(child: RendererObject) {
    if (child.parentRendererData is ZStackRendererData) return

    if (child.parentRendererData is ContainerRendererData<*>) {
      val childParentRendererData = child.parentRendererData!! as ContainerRendererData<*>
      val previousSibling = childParentRendererData.previousSibling
      val nextSibling = childParentRendererData.nextSibling
      val offset = childParentRendererData.offset

      child.parentRendererData =
        ZStackRendererData().apply {
          this.previousSibling = previousSibling as BoxRenderer?
          this.nextSibling = nextSibling as BoxRenderer?
          this.offset = offset
        }

      return
    }

    child.parentRendererData = ZStackRendererData()
  }

  private fun computeSize(
    constraints: BoxConstraints,
    layoutChild: (child: BoxRenderer, constraints: BoxConstraints) -> Size
  ): Size {
    resolve()

    if (childCount == 0) return if (constraints.biggest.isFinite()) constraints.biggest else constraints.smallest

    var hasNonPositionedChildren = false

    var width = constraints.minWidth
    var height = constraints.minHeight

    val nonPositionedConstraints =
      when (fit) {
        ZStackFit.Loose -> constraints.loosen()
        ZStackFit.Expand -> BoxConstraints.tight(constraints.biggest)
        ZStackFit.PassThrough -> constraints
      }

    var child = firstChild

    while (child != null) {
      val childParentRendererData = child.parentRendererData!! as ZStackRendererData

      if (!childParentRendererData.isPositioned) {
        hasNonPositionedChildren = true

        val childSize = layoutChild(child, nonPositionedConstraints)

        width = maxOf(width, childSize.width)
        height = maxOf(height, childSize.height)
      }

      child = childParentRendererData.nextSibling
    }

    return (
      if (hasNonPositionedChildren) {
        assert(width == constraints.constrainWidth(width))
        assert(height == constraints.constrainHeight(height))

        Size(width, height)
      } else {
        constraints.biggest
      }
    ).apply { assert(isFinite()) }
  }

  override fun computeDryLayout(constraints: BoxConstraints) =
    computeSize(constraints) { child, childConstraints ->
      child.getDryLayout(childConstraints)
    }

  override fun performLayout() {
    val constraints = getConstraints()

    size =
      computeSize(constraints) { child, childConstraints ->
        child.layout(childConstraints, true)

        child.size
      }

    var child = firstChild

    while (child != null) {
      val childParentRendererData = child.parentRendererData!! as ZStackRendererData

      if (!childParentRendererData.isPositioned) {
        childParentRendererData.offset = resolvedAlignment!!.alongOffset((size - child.size).toPoint())
      } else {
        hasVisualOverflow = layoutPositionedChild(
          child,
          childParentRendererData,
          size,
          resolvedAlignment!!
        ) || hasVisualOverflow
      }

      child = childParentRendererData.nextSibling
    }
  }

  protected fun paintStack(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    var child = firstChild

    while (child != null) {
      val childParentRendererData = child.parentRendererData!! as ZStackRendererData

      currentScope.nestPositionalScope(childParentRendererData.offset) {
        child!!.paint(d, currentScope)
      }

      child = childParentRendererData.nextSibling
    }
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    if (hasVisualOverflow && !ignoreVisualOverflow) {
      println("ZStack has visual overflow")
    }

    paintStack(d, currentScope)
  }

  override fun attach(context: RenderGlobalContext) {
    super.attach(context)

    containerRendererImpl.attach(context)
  }

  override fun detach() {
    super.detach()

    containerRendererImpl.detach()
  }

  override fun redepthChildren() {
    super.redepthChildren()

    containerRendererImpl.redepthChildren()
  }

  override fun visitChildren(visitor: (RendererObject) -> Unit) {
    super.visitChildren(visitor)

    containerRendererImpl.visitChildren(visitor)
  }
}
