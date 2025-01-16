package dev.wycey.mido.leinwand.components

import de.milchreis.uibooster.UiBooster
import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.input.Button
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons

internal class LayerList(
  private val instanceId: Int
) : StatefulComponent("layerList$instanceId") {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!
    val currentLayer by computed { handle.layers[handle.activeLayerIndex] }

    val disableAdd = createFunction { handle.layers.size >= dev.wycey.mido.leinwand.LeinwandHandle.MAX_LAYERS }

    val disableDelete =
      createFunction { handle.layers.size == 1 || currentLayer.name == "Root Layer" || currentLayer.lock }

    val disableUp =
      createFunction {
        handle.activeLayerIndex == handle.layers.size - 1 || currentLayer.name == "Root Layer" || currentLayer.lock
      }

    val disableDown =
      createFunction {
        currentLayer.name == "Root Layer" ||
          currentLayer.lock ||
          handle.layers[handle.activeLayerIndex - 1].name == "Root Layer"
      }

    effect { handle.waitForLayerState }

    val header =
      HStack(
        listOf(
          Button(
            Text("+", Styles.text),
            Styles.sidebarButton.let {
              if (disableAdd()) it.copy(disabled = true) else it
            },
            onClick = onClick@{ _, type ->
              if (type.button != MouseButtons.LEFT || disableAdd()) return@onClick

              val newName = UiBooster().showTextInputDialog("新しいレイヤーの名前:") ?: return@onClick

              handle.addLayer(newName)
            }
          ),
          VirtualBox(width = 8f),
          Button(
            Text("-", Styles.text),
            Styles.sidebarButton.let {
              if (disableDelete()) it.copy(disabled = true) else it
            },
            onClick = onClick@{ _, type ->
              if (type.button != MouseButtons.LEFT || disableDelete()) return@onClick

              if (!UiBooster().showConfirmDialog(
                  "本当にレイヤーを削除しますか?",
                  "レイヤー${currentLayer.name}を削除"
                )
              ) {
                return@onClick
              }

              handle.removeLayer(handle.activeLayerIndex)
            },
            key = "deleteLayer${handle.activeLayerIndex}${handle.waitForLayerState}"
          ),
          VirtualBox(width = 8f),
          Button(
            Text("Up", Styles.text),
            Styles.sidebarButton.let {
              if (disableUp()) it.copy(disabled = true) else it
            },
            onClick = onClick@{ _, type ->
              if (type.button != MouseButtons.LEFT || disableUp()) return@onClick

              handle.moveLayerUp(handle.activeLayerIndex)
            },
            key = "moveLayerUp${handle.activeLayerIndex}${handle.waitForLayerState}"
          ),
          VirtualBox(width = 8f),
          Button(
            Text("Down", Styles.text),
            Styles.sidebarButton.let {
              if (disableDown()) it.copy(disabled = true) else it
            },
            onClick = onClick@{ _, type ->
              if (type.button != MouseButtons.LEFT || disableDown()) return@onClick

              handle.moveLayerDown(handle.activeLayerIndex)
            },
            key = "moveLayerDown${handle.activeLayerIndex}${handle.waitForLayerState}"
          )
        )
      )

    val line =
      VirtualBox(
        height = 1f,
        child =
          Box(
            fill = 0xff444446.toInt()
          )
      )

    return VStack(
      {
        val list = mutableListOf<Component>(header, VirtualBox(height = 8f), line)

        list.addAll(
          handle.layers
            .withIndex()
            .flatMap { (i, layer) ->
              listOf(
                LayerItem(instanceId, layer, i == handle.activeLayerIndex) {
                  handle.selectLayer(i)
                },
                line
              )
            }.asReversed()
        )

        list
      },
      "layerList$instanceId${handle.layers.size}",
      crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Stretch
    )
  }
}
