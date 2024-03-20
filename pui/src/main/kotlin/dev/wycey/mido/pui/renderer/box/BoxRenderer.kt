package dev.wycey.mido.pui.renderer.box

import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.data.BoxRendererData

open class BoxRenderer : RendererObject() {
  private var _size: Size? = null

  var size: Size
    get() = _size ?: throw IllegalStateException("Size is not set")
    set(value) {
      _size = value
    }

  private var cachedDryLayoutSizes: MutableMap<BoxConstraints, Size>? = null

  protected fun getConstraints(): BoxConstraints = constraints as BoxConstraints

  override fun setupParentRendererData(child: RendererObject) {
    if (child.parentRendererData !is BoxRendererData) {
      child.parentRendererData = BoxRendererData()
    }
  }

  private fun clearCachedData(): Boolean {
    if (cachedDryLayoutSizes != null && cachedDryLayoutSizes!!.isNotEmpty()) {
      cachedDryLayoutSizes!!.clear()

      return true
    }

    return false
  }

  override fun markNeedsLayout() {
    if (clearCachedData() && parent is RendererObject) {
      markParentNeedsLayout()
    }

    super.markNeedsLayout()
  }

  protected open fun computeDryLayout(constraints: BoxConstraints): Size {
    println("Warning: computeDryLayout() is not implemented for ${this::class.simpleName}")

    return Size.ZERO
  }

  private fun _computeDryLayout(constraints: BoxConstraints): Size = computeDryLayout(constraints)

  open fun getDryLayout(constraints: BoxConstraints): Size {
    if (cachedDryLayoutSizes == null) {
      cachedDryLayoutSizes = mutableMapOf()
    }

    val result = cachedDryLayoutSizes!!.computeIfAbsent(constraints) { _computeDryLayout(it) }

    return result
  }

  override fun performResize() {
    _size = _computeDryLayout(getConstraints())
  }

  fun hasSize() = _size != null

  open fun getParentAbsolutePosition(): Point {
    var parent = parent

    while (parent != null) {
      if (parent is BoxRenderer) {
        val position = parent.safeGetAbsolutePosition()

        if (position != null) {
          return position
        }
      }

      parent = parent.parent
    }

    return Point.ZERO
  }

  open fun safeGetAbsolutePosition(): Point? {
    return try {
      getAbsolutePosition()
    } catch (e: IllegalStateException) {
      null
    }
  }

  open fun getAbsolutePosition(): Point {
    val parentRendererData =
      parentRendererData as? BoxRendererData
        ?: throw IllegalStateException(
          "Parent renderer data is not a BoxRendererData, but ${parentRendererData?.javaClass?.simpleName}"
        )

    return getParentAbsolutePosition() + parentRendererData.offset
  }
}
