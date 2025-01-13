package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.ParentRendererDataComponent
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.data.ZStackRendererData

class Positioned
  @JvmOverloads
  constructor(
    child: Component,
    val left: Float? = null,
    val top: Float? = null,
    val right: Float? = null,
    val bottom: Float? = null,
    val width: Float? = null,
    val height: Float? = null,
    key: String? = null
  ) : ParentRendererDataComponent<ZStackRendererData>(child, key) {
    companion object {
      @JvmStatic
      @JvmOverloads
      fun full(
        child: Component,
        left: Float = 0f,
        top: Float = 0f,
        right: Float = 0f,
        bottom: Float = 0f,
        key: String? = null
      ) = Positioned(child, left, top, right, bottom, key = key)
    }

    init {
      assert(left == null || right == null || width == null) { "Cannot specify both left and right or width" }
      assert(top == null || bottom == null || height == null) { "Cannot specify both top and bottom or height" }
    }

    override fun applyParentRendererData(renderer: RendererObject) {
      val parentRendererData = renderer.parentRendererData!! as ZStackRendererData

      var needsLayout = false

      if (parentRendererData.left != left) {
        parentRendererData.left = left
        needsLayout = true
      }

      if (parentRendererData.top != top) {
        parentRendererData.top = top
        needsLayout = true
      }

      if (parentRendererData.right != right) {
        parentRendererData.right = right
        needsLayout = true
      }

      if (parentRendererData.bottom != bottom) {
        parentRendererData.bottom = bottom
        needsLayout = true
      }

      if (parentRendererData.width != width) {
        parentRendererData.width = width
        needsLayout = true
      }

      if (parentRendererData.height != height) {
        parentRendererData.height = height
        needsLayout = true
      }

      if (needsLayout) {
        val targetParent = renderer.parent

        if (targetParent is RendererObject) {
          targetParent.markNeedsLayout()
        }
      }
    }
  }
