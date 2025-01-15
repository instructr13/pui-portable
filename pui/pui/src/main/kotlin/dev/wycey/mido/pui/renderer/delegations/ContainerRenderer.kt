package dev.wycey.mido.pui.renderer.delegations

import dev.wycey.mido.pui.renderer.RenderGlobalContext
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.RendererVisitor

public interface ContainerRendererContract<ChildType : RendererObject> {
  public val childCount: Int
  public val firstChild: ChildType?
  public val lastChild: ChildType?

  public fun childBefore(child: ChildType): ChildType?

  public fun childAfter(child: ChildType): ChildType?

  public fun insert(
    child: ChildType,
    after: ChildType? = null
  )

  public fun add(child: ChildType)

  public fun addAll(children: List<ChildType>)

  public fun remove(child: ChildType)

  public fun removeAll()

  public fun move(
    child: ChildType,
    after: ChildType? = null
  )
}

public class ContainerRendererImpl<
  ChildType : RendererObject,
  ParentDataType : ContainerRendererDataContract<ChildType>
  > :
  ContainerRendererContract<ChildType> {
  override val childCount: Int
    get() = _childCount

  override val firstChild: ChildType?
    get() = _firstChild

  override val lastChild: ChildType?
    get() = _lastChild

  internal lateinit var that: RendererObject

  private var _childCount = 0
  private var _firstChild: ChildType? = null
  private var _lastChild: ChildType? = null

  public val children: List<ChildType>
    get() {
      val children = mutableListOf<ChildType>()

      var child = _firstChild

      while (child != null) {
        children.add(child)

        val childParentRendererData = child.parentRendererData!! as ParentDataType

        child = childParentRendererData.nextSibling
      }

      return children
    }

  override fun childBefore(child: ChildType): ChildType? {
    val childParentRendererData = child.parentRendererData!! as ParentDataType

    return childParentRendererData.previousSibling
  }

  override fun childAfter(child: ChildType): ChildType? {
    val childParentRendererData = child.parentRendererData!! as ParentDataType

    return childParentRendererData.nextSibling
  }

  private fun insertIntoChildList(
    child: ChildType,
    after: ChildType?
  ) {
    val childParentRendererData = child.parentRendererData!! as ParentDataType

    assert(childParentRendererData.previousSibling == null)
    assert(childParentRendererData.nextSibling == null)

    _childCount++

    assert(_childCount > 0)

    if (after == null) {
      childParentRendererData.nextSibling = _firstChild

      if (_firstChild != null) {
        val firstChildParentRendererData =
          _firstChild!!.parentRendererData!! as ParentDataType

        firstChildParentRendererData.previousSibling = child
      }

      _firstChild = child
      _lastChild = _lastChild ?: child
    } else {
      assert(_firstChild != null)
      assert(_lastChild != null)

      val afterParentRendererData = after.parentRendererData!! as ParentDataType

      if (afterParentRendererData.nextSibling == null) {
        assert(after == _lastChild)

        childParentRendererData.previousSibling = after
        afterParentRendererData.nextSibling = child
        _lastChild = child

        return
      }

      childParentRendererData.nextSibling = afterParentRendererData.nextSibling
      childParentRendererData.previousSibling = after

      val childPreviousSiblingParentRendererData =
        childParentRendererData.previousSibling!!.parentRendererData!! as ParentDataType

      val childNextSiblingParentRendererData =
        childParentRendererData.nextSibling!!.parentRendererData!! as ParentDataType

      childPreviousSiblingParentRendererData.nextSibling = child
      childNextSiblingParentRendererData.previousSibling = child

      assert(afterParentRendererData.nextSibling == child)
    }
  }

  override fun insert(
    child: ChildType,
    after: ChildType?
  ) {
    assert(child != this) { "Cannot insert a renderer into itself" }
    assert(after != this) { "Cannot insert a renderer into itself" }
    assert(child != after) { "Cannot insert a renderer after itself" }

    assert(child != _firstChild)
    assert(child != _lastChild)

    that.setupParentRendererData(child)
    that.insertChild(child)

    insertIntoChildList(child, after)
  }

  override fun add(child: ChildType) {
    insert(child, after = _lastChild)
  }

  override fun addAll(children: List<ChildType>): Unit = children.forEach(::add)

  private fun removeFromChildList(child: ChildType) {
    val childParentRendererData = child.parentRendererData!! as ParentDataType

    if (childParentRendererData.previousSibling == null) {
      _firstChild = childParentRendererData.nextSibling
    } else {
      val childPreviousSiblingParentRendererData =
        childParentRendererData.previousSibling!!.parentRendererData!! as ParentDataType

      childPreviousSiblingParentRendererData.nextSibling = childParentRendererData.nextSibling
    }

    if (childParentRendererData.nextSibling == null) {
      _lastChild = childParentRendererData.previousSibling
    } else {
      val childNextSiblingParentRendererData =
        childParentRendererData.nextSibling!!.parentRendererData!! as ParentDataType

      childNextSiblingParentRendererData.previousSibling = childParentRendererData.previousSibling
    }

    childParentRendererData.previousSibling = null
    childParentRendererData.nextSibling = null

    _childCount--
  }

  override fun remove(child: ChildType) {
    removeFromChildList(child)

    that.dropChild(child)
  }

  override fun removeAll() {
    var child = _firstChild

    while (child != null) {
      val childParentRendererData = child.parentRendererData!! as ParentDataType
      val next = childParentRendererData.nextSibling

      childParentRendererData.previousSibling = null
      childParentRendererData.nextSibling = null

      that.dropChild(child)

      child = next
    }

    _firstChild = null
    _lastChild = null

    _childCount = 0
  }

  override fun move(
    child: ChildType,
    after: ChildType?
  ) {
    val childParentRendererData = child.parentRendererData!! as ParentDataType

    if (childParentRendererData.previousSibling == after) {
      return
    }

    removeFromChildList(child)
    insertIntoChildList(child, after)

    that.markNeedsLayout()
  }

  public fun attach(context: RenderGlobalContext) {
    var child = _firstChild

    while (child != null) {
      child.attach(context)

      val childParentRendererData = child.parentRendererData!! as ParentDataType

      child = childParentRendererData.nextSibling
    }
  }

  public fun detach() {
    var child = _firstChild

    while (child != null) {
      child.detach()

      val childParentRendererData = child.parentRendererData!! as ParentDataType

      child = childParentRendererData.nextSibling
    }
  }

  public fun redepthChildren() {
    var child = _firstChild

    while (child != null) {
      that.redepthChild(child)

      val childParentRendererData = child.parentRendererData!! as ParentDataType

      child = childParentRendererData.nextSibling
    }
  }

  public fun visitChildren(visitor: RendererVisitor) {
    var child = _firstChild

    while (child != null) {
      visitor(child)

      val childParentRendererData = child.parentRendererData!! as ParentDataType

      child = childParentRendererData.nextSibling
    }
  }
}
