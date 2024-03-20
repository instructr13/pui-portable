package dev.wycey.mido.pui.examples.colors

import dev.wycey.mido.pui.components.rendering.RendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.rendering.EmptyRendererElement
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.BoxRenderer

internal class HSLuvRenderColorPickerBox(
  private val coordinate: Point,
  private val hue: Float,
  private val pickedColorCircleRadius: Float,
  key: String? = null
) : RendererComponent(key) {
  override fun createElement() = EmptyRendererElement<BoxRenderer>(this)

  override fun createRenderer(context: BuildContext) =
    HSLuvColorPickerBoxRenderer(coordinate, hue, pickedColorCircleRadius)

  override fun updateRenderer(
    context: BuildContext,
    renderer: RendererObject
  ) {
    (renderer as HSLuvColorPickerBoxRenderer).coordinate = coordinate
    renderer.hue = hue
    renderer.pickedColorCircleRadius = pickedColorCircleRadius
  }
}
