package dev.wycey.mido.pui.renderer.layout

import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.RenderGlobalContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.data.BoxRendererData
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererContract
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererData
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererDataContract
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererImpl
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer

public enum class StackDirection {
  Horizontal,
  Vertical
}

public enum class StackMainAxisAlignment {
  Start,
  End,
  Center,
  SpaceBetween,
  SpaceAround,
  SpaceEvenly
}

public enum class StackCrossAxisAlignment {
  Start,
  End,
  Center,
  Stretch
}

public enum class StackFit {
  Expand,
  Loose
}

public data class StackParentRendererData(
  var flex: Int? = null,
  var fit: StackFit = StackFit.Expand
) : BoxRendererData(),
  ContainerRendererDataContract<BoxRenderer> by ContainerRendererData()

private data class LayoutSizes(
  val main: Float,
  val cross: Float,
  val allocated: Float
)

public class StackRenderer(
  initialDirection: StackDirection,
  initialMainAxisAlignment: StackMainAxisAlignment = StackMainAxisAlignment.Start,
  initialCrossAxisAlignment: StackCrossAxisAlignment = StackCrossAxisAlignment.Start,
  children: List<BoxRenderer> = emptyList(),
  private val containerRendererImpl: ContainerRendererImpl<BoxRenderer, StackParentRendererData> =
    ContainerRendererImpl()
) : BoxRenderer(),
  ContainerRendererContract<BoxRenderer> by containerRendererImpl {
  init {
    containerRendererImpl.that = this

    addAll(children)
  }

  public var direction: StackDirection = initialDirection
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayout()
    }

  public var mainAxisAlignment: StackMainAxisAlignment = initialMainAxisAlignment
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayout()
    }

  public var crossAxisAlignment: StackCrossAxisAlignment = initialCrossAxisAlignment
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayout()
    }

  override fun setupParentRendererData(child: RendererObject) {
    if (child.parentRendererData is StackParentRendererData) return

    if (child.parentRendererData is ContainerRendererData<*>) {
      val childParentRendererData = child.parentRendererData!! as ContainerRendererData<*>
      val previousSibling = childParentRendererData.previousSibling
      val nextSibling = childParentRendererData.nextSibling
      val offset = childParentRendererData.offset

      child.parentRendererData =
        StackParentRendererData().apply {
          this.previousSibling = previousSibling as BoxRenderer?
          this.nextSibling = nextSibling as BoxRenderer?
          this.offset = offset
        }

      return
    }

    child.parentRendererData = StackParentRendererData()
  }

  private fun getFlex(child: BoxRenderer): Int = (child.parentRendererData as StackParentRendererData).flex ?: 0

  private fun getFit(child: BoxRenderer): StackFit = (child.parentRendererData as StackParentRendererData).fit

  private fun getCrossSize(size: Size): Float =
    when (direction) {
      StackDirection.Horizontal -> size.height
      StackDirection.Vertical -> size.width
    }

  private fun getMainSize(size: Size): Float =
    when (direction) {
      StackDirection.Horizontal -> size.width
      StackDirection.Vertical -> size.height
    }

  private fun computeSizes(
    constraints: BoxConstraints,
    layoutChild: (child: BoxRenderer, constraints: BoxConstraints) -> Size
  ): LayoutSizes {
    var totalFlex = 0
    val maxMainSize =
      if (direction == StackDirection.Horizontal) constraints.maxWidth else constraints.maxHeight
    val canFlex = maxMainSize < Float.POSITIVE_INFINITY

    var crossSize = 0f
    var allocatedSize = 0f

    var child = firstChild
    var lastFlexChild: BoxRenderer? = null

    while (child != null) {
      val flex = getFlex(child)

      if (flex > 0) {
        totalFlex += flex
        lastFlexChild = child
      } else {
        val innerConstraints: BoxConstraints =
          if (crossAxisAlignment == StackCrossAxisAlignment.Stretch) {
            if (direction == StackDirection.Horizontal) {
              BoxConstraints.tightFor(
                height = constraints.maxHeight
              )
            } else {
              BoxConstraints.tightFor(width = constraints.maxWidth)
            }
          } else {
            if (direction == StackDirection.Horizontal) {
              BoxConstraints(maxHeight = constraints.maxHeight)
            } else {
              BoxConstraints(maxWidth = constraints.maxWidth)
            }
          }

        val childSize = layoutChild(child, innerConstraints)

        allocatedSize += getMainSize(childSize)
        crossSize = maxOf(crossSize, getCrossSize(childSize))
      }

      val childParentRendererData =
        child.parentRendererData!! as StackParentRendererData

      child = childParentRendererData.nextSibling
    }

    val remainingSpace = maxOf(0f, (if (canFlex) maxMainSize else 0f) - allocatedSize)
    var allocatedFlexSpace = 0f

    if (totalFlex > 0) {
      val spacePerFlex = if (canFlex) remainingSpace / totalFlex else Float.NaN

      child = firstChild

      while (child != null) {
        val flex = getFlex(child)

        if (flex > 0) {
          val maxChildExtent =
            if (canFlex) {
              if (child == lastFlexChild) {
                remainingSpace - allocatedFlexSpace
              } else {
                flex * spacePerFlex
              }
            } else {
              Float.POSITIVE_INFINITY
            }

          val minChildExtent =
            when (getFit(child)) {
              StackFit.Expand -> {
                maxChildExtent
              }

              StackFit.Loose -> {
                0f
              }
            }

          val innerConstraints: BoxConstraints =
            if (crossAxisAlignment == StackCrossAxisAlignment.Stretch) {
              if (direction == StackDirection.Horizontal) {
                BoxConstraints(
                  minChildExtent,
                  maxChildExtent,
                  constraints.maxHeight,
                  constraints.maxHeight
                )
              } else {
                BoxConstraints(
                  constraints.maxWidth,
                  constraints.maxWidth,
                  minChildExtent,
                  maxChildExtent
                )
              }
            } else {
              if (direction == StackDirection.Horizontal) {
                BoxConstraints(
                  minWidth = minChildExtent,
                  maxWidth = maxChildExtent,
                  maxHeight = constraints.maxHeight
                )
              } else {
                BoxConstraints(
                  maxWidth = constraints.maxWidth,
                  minHeight = minChildExtent,
                  maxHeight = maxChildExtent
                )
              }
            }

          val childSize = layoutChild(child, innerConstraints)
          val childMainSize = getMainSize(childSize)

          allocatedSize += childMainSize
          allocatedFlexSpace += maxChildExtent

          crossSize = maxOf(crossSize, getCrossSize(childSize))
        }

        val childParentRendererData =
          child.parentRendererData!! as StackParentRendererData

        child = childParentRendererData.nextSibling
      }
    }

    return LayoutSizes(
      if (canFlex) maxMainSize else allocatedSize,
      crossSize,
      allocatedSize
    )
  }

  override fun computeDryLayout(constraints: BoxConstraints): Size {
    val sizes =
      computeSizes(constraints) { child, innerConstraints ->
        child.getDryLayout(innerConstraints)
      }

    return when (direction) {
      StackDirection.Horizontal -> Size(sizes.main, sizes.cross)
      StackDirection.Vertical -> Size(sizes.cross, sizes.main)
    }
  }

  override fun performLayout() {
    val constraints = getConstraints()

    val sizes =
      computeSizes(constraints) { child, innerConstraints ->
        child.layout(innerConstraints, true)

        child.size
      }

    val allocatedSize = sizes.allocated
    var actualSize = sizes.main
    var crossSize = sizes.cross

    when (direction) {
      StackDirection.Horizontal -> {
        size = constraints.constrain(Size(actualSize, crossSize))

        actualSize = size.width
        crossSize = size.height
      }

      StackDirection.Vertical -> {
        size = constraints.constrain(Size(crossSize, actualSize))

        actualSize = size.height
        crossSize = size.width
      }
    }

    val actualSizeDelta = actualSize - allocatedSize
    val remainingSpace = maxOf(0f, actualSizeDelta)

    val (leadingSpace, betweenSpace) =
      when (mainAxisAlignment) {
        StackMainAxisAlignment.Start -> {
          0f to 0f
        }

        StackMainAxisAlignment.End -> {
          remainingSpace to 0f
        }

        StackMainAxisAlignment.Center -> {
          remainingSpace / 2 to 0f
        }

        StackMainAxisAlignment.SpaceBetween -> {
          0f to if (childCount > 1) remainingSpace / (childCount - 1) else 0f
        }

        StackMainAxisAlignment.SpaceAround -> {
          val between = if (childCount > 0) remainingSpace / childCount else 0f

          between / 2 to between
        }

        StackMainAxisAlignment.SpaceEvenly -> {
          val between = if (childCount > 0) remainingSpace / (childCount + 1) else 0f

          between to between
        }
      }

    var childMainPosition = leadingSpace
    var child = firstChild

    while (child != null) {
      val childParentRendererData =
        child.parentRendererData!! as StackParentRendererData

      val childCrossPosition =
        when (crossAxisAlignment) {
          StackCrossAxisAlignment.Start -> {
            0f
          }

          StackCrossAxisAlignment.End -> {
            crossSize - getCrossSize(child.size)
          }

          StackCrossAxisAlignment.Center -> {
            (crossSize - getCrossSize(child.size)) / 2
          }

          StackCrossAxisAlignment.Stretch -> {
            0f
          }
        }

      when (direction) {
        StackDirection.Horizontal -> {
          childParentRendererData.offset = Point(childMainPosition, childCrossPosition)
        }

        StackDirection.Vertical -> {
          childParentRendererData.offset = Point(childCrossPosition, childMainPosition)
        }
      }

      childMainPosition += getMainSize(child.size) + betweenSpace

      child = childParentRendererData.nextSibling
    }
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    var child = firstChild

    while (child != null) {
      val childParentRendererData =
        child.parentRendererData!! as StackParentRendererData

      currentScope.nestPositionalScope(childParentRendererData.offset) {
        child.paint(d, it)
      }

      child = childParentRendererData.nextSibling
    }
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
