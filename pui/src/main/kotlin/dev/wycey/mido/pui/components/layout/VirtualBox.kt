package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.ConstrainedBoxRenderer

class VirtualBox
  @JvmOverloads
  constructor(
    child: Component? = null,
    val width: Float? = null,
    val height: Float? = null,
    key: String? = null
  ) :
  SingleChildRendererComponent(key, child) {
    companion object {
      @JvmStatic
      @JvmOverloads
      fun expand(
        child: Component? = null,
        key: String? = null
      ) = VirtualBox(child, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, key)

      @JvmStatic
      @JvmOverloads
      fun shrink(
        child: Component? = null,
        key: String? = null
      ) = VirtualBox(child, 0f, 0f, key)

      @JvmStatic
      @JvmOverloads
      fun square(
        child: Component? = null,
        size: Float,
        key: String? = null
      ) = VirtualBox(child, size, size, key)

      @JvmStatic
      @JvmOverloads
      fun fromSize(
        child: Component? = null,
        size: Size,
        key: String? = null
      ) = VirtualBox(child, size.width, size.height, key)
    }

    private val additionalConstraints = BoxConstraints.tightFor(width, height)

    override fun createRenderer(context: BuildContext) = ConstrainedBoxRenderer(additionalConstraints)

    override fun updateRenderer(
      context: BuildContext,
      renderer: RendererObject
    ) {
      (renderer as ConstrainedBoxRenderer).additionalConstraints = additionalConstraints
    }
  }
