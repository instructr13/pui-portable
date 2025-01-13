package dev.wycey.mido.leinwand

import com.github.ajalt.colormath.model.HSLuv
import dev.wycey.mido.leinwand.layers.Layer
import dev.wycey.mido.leinwand.tools.Tool
import dev.wycey.mido.leinwand.tools.brush.Eraser
import dev.wycey.mido.leinwand.tools.brush.Marker
import dev.wycey.mido.leinwand.tools.brush.NormalBrush
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.state.signals.computed
import dev.wycey.mido.pui.state.signals.signal
import dev.wycey.mido.pui.state.signals.untracked
import processing.core.PApplet
import processing.core.PGraphics

internal class LeinwandHandle(
  val applet: PApplet,
  initialForegroundColor: HSLuv = HSLuv(0f, 0f, 0f),
  initialBackgroundColor: HSLuv = HSLuv(0f, 0f, 100f),
  val statusQueue: ArrayDeque<String> = ArrayDeque(),
  val maxUndos: Int = 32
) {
  companion object {
    @JvmField
    val instances = mutableMapOf<Int, LeinwandHandle>()

    const val MAX_LAYERS = 6
  }

  val canvasSize = Size(600f, 600f)

  val tools =
    listOf<Tool>(
      NormalBrush(this),
      Eraser(this),
      Marker(this)
    )

  lateinit var currentBaseLayer: PGraphics
  var currentToolIndex by signal(0)
  val currentTool by computed { tools[currentToolIndex] }
  var foregroundColor by signal(initialForegroundColor)
  var backgroundColor by signal(initialBackgroundColor)
  var selectingForeground by signal(true)
  var activeLayerIndex by signal(0)
  var waitForLayerState by signal(0)
  var statusText by signal("")

  val layers: MutableList<Layer> =
    mutableListOf(
      Layer(applet, "Root Layer", this)
    )

  fun selectLayer(index: Int) {
    val targetLayer = layers[index]

    if (targetLayer.reference) targetLayer.reference = false

    activeLayerIndex = index
  }

  fun addLayer(name: String) {
    layers.add(
      activeLayerIndex + 1,
      Layer(
        applet,
        name.trim().ifEmpty {
          "Layer ${layers.size + 1}"
        },
        this
      )
    )

    selectLayer(activeLayerIndex + 1)

    waitForLayerState++
  }

  fun renameLayer(
    id: String,
    name: String
  ) {
    layers.find { it.id == id }?.name = name

    waitForLayerState++
  }

  fun renameLayer(
    index: Int,
    name: String
  ) {
    layers[index].name = name

    waitForLayerState++
  }

  fun moveLayerUp(index: Int) {
    if (index == layers.size - 1) return

    val layer = layers.removeAt(index)

    layers.add(index + 1, layer)

    selectLayer(index + 1)

    waitForLayerState++
  }

  fun moveLayerDown(index: Int) {
    if (index == 0) return

    val layer = layers.removeAt(index)

    layers.add(index - 1, layer)

    selectLayer(index - 1)

    waitForLayerState++
  }

  fun removeLayer(index: Int) {
    if (untracked { activeLayerIndex } == index) {
      activeLayerIndex = untracked { activeLayerIndex } - 1
    }

    layers.removeAt(index)

    waitForLayerState++
  }

  fun toggleReferenceLayer(id: String) {
    val targetLayer = layers.find { it.id == id } ?: return

    if (targetLayer.reference) {
      targetLayer.reference = false
    } else {
      layers.find { it.reference }?.reference = false

      targetLayer.reference = true
    }
  }
}
