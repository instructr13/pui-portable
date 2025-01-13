package dev.wycey.mido.leinwand.components

import de.milchreis.uibooster.UiBooster
import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.leinwand.layers.Layer
import dev.wycey.mido.leinwand.util.BlendModes
import dev.wycey.mido.pui.bridges.BridgeBase.Companion.applet
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.components.input.Button
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.util.processing.CursorType
import processing.core.PApplet

internal class LayerItem(
  private val instanceId: Int,
  private val layer: Layer,
  private val active: Boolean = false,
  private val onSelect: () -> Unit
) :
  StatefulComponent("layerItem${layer.id}$active") {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!

    var hovering by signal(false)

    val onHover =
      createFunction {
        CursorType.Hand.apply(applet)

        hovering = true
      }

    val onLeave =
      createFunction {
        CursorType.Arrow.apply(applet)

        hovering = false
      }

    return Box(
      fill =
        if (active) {
          0x22ffffff
        } else if (hovering) {
          0x08ffffff
        } else {
          0x00ffffff
        },
      child =
        Padding(
          EdgeInsets.symmetric(4f, 8f),
          HStack(
            listOf(
              GestureListener(
                onClick = onClick@{ _, type ->
                  if (type.button != MouseButtons.LEFT) return@onClick

                  onSelect()
                },
                onHover = onHover@{ _, _ ->
                  onHover()
                },
                onLeave = onLeave@{ _, _ ->
                  onLeave()
                },
                child =
                  HStack(
                    listOf(
                      VirtualBox(
                        height = 40f,
                        width = 40f,
                        child =
                          Box(
                            fill = 0xffffffff.toInt(),
                            stroke = 0xff000000.toInt(),
                            strokeWeight = 2f,
                            additionalPaint = { d, _, _ ->
                              d.applet.image(layer.thumbnail, 0f, 0f)
                            }
                          )
                      ),
                      VirtualBox(width = 8f)
                    )
                  )
              ),
              VStack(
                listOf(
                  GestureListener(
                    onClick = onClick@{ _, type ->
                      if (type.button != MouseButtons.LEFT) return@onClick

                      onSelect()
                    },
                    onHover = onHover@{ _, _ ->
                      onHover()
                    },
                    onLeave = onLeave@{ _, _ ->
                      onLeave()
                    },
                    child =
                      VStack(
                        listOf(
                          if (layer.name == "Root Layer") {
                            Text(layer.name, Styles.text)
                          } else {
                            GestureListener(
                              onClick = onClick@{ _, type ->
                                if (type.button != MouseButtons.LEFT || type.count < 1) return@onClick

                                val newName =
                                  (
                                    UiBooster().showTextInputDialog(
                                      "新しいレイヤーの名前:"
                                    ) ?: return@onClick
                                  ).trim()

                                if (newName.isEmpty()) return@onClick

                                handle.renameLayer(layer.id, newName)

                                handle.waitForLayerState++
                              },
                              onHover = onHover@{ _, _ ->
                                CursorType.Text.apply(applet)
                              },
                              onLeave = onLeave@{ _, _ ->
                                CursorType.Arrow.apply(applet)
                              },
                              child =
                                Text(
                                  if (layer.blendMode == PApplet.NORMAL) {
                                    layer.name
                                  } else {
                                    "${layer.name} (${BlendModes.blendModeToBlendName[layer.blendMode]})"
                                  },
                                  Styles.text,
                                  key = "layerName${layer.name}${layer.blendMode}"
                                )
                            )
                          },
                          VirtualBox(height = 4f)
                        ),
                        crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Stretch
                      )
                  ),
                  HStack(
                    {
                      val list = mutableListOf<Component>()

                      list.addAll(
                        listOf(
                          Button(
                            Text("Hide", Styles.text),
                            Styles.sidebarButton.let {
                              if (layer.hidden) {
                                it.copy(normalColor = 0xff1e1f22.toInt())
                              } else {
                                it
                              }
                            },
                            onClick = onClick@{ _, type ->
                              if (type.button != MouseButtons.LEFT) return@onClick

                              layer.hidden = !layer.hidden

                              handle.waitForLayerState++
                            },
                            key = "hideLayer${layer.id}${layer.hidden}"
                          ),
                          VirtualBox(width = 4f)
                        )
                      )

                      if (layer.name != "Root Layer") {
                        list.addAll(
                          listOf(
                            Button(
                              Text("Lock", Styles.text),
                              Styles.sidebarButton.let {
                                if (layer.lock) {
                                  it.copy(normalColor = 0xff1e1f22.toInt())
                                } else {
                                  it
                                }
                              },
                              onClick = onClick@{ _, type ->
                                if (type.button != MouseButtons.LEFT) return@onClick

                                layer.lock = !layer.lock

                                handle.waitForLayerState++
                              },
                              key = "lockLayer${layer.id}${layer.lock}"
                            ),
                            VirtualBox(width = 4f)
                          )
                        )
                      }

                      list.add(
                        Button(
                          Text("Ref.", Styles.text),
                          Styles.sidebarButton.let {
                            if (layer.reference) {
                              it.copy(normalColor = 0xff1e1f22.toInt())
                            } else {
                              it
                            }
                          },
                          onClick = onClick@{ _, type ->
                            if (type.button != MouseButtons.LEFT) return@onClick
                            if (handle.layers[handle.activeLayerIndex] == layer) return@onClick

                            handle.toggleReferenceLayer(layer.id)

                            handle.waitForLayerState++
                          },
                          key = "refLayer${layer.id}${handle.waitForLayerState}"
                        )
                      )

                      if (layer.name != "Root Layer") {
                        list.addAll(
                          listOf(
                            VirtualBox(width = 4f),
                            HStack(
                              listOf(
                                Button(
                                  Text("Blend", Styles.text),
                                  Styles.topBarButton.copy(
                                    childPadding = EdgeInsets.symmetric(4f, 2f)
                                  ),
                                  onClick = onClick@{ _, type ->
                                    if (type.button != MouseButtons.LEFT) return@onClick

                                    val newBlend =
                                      UiBooster().showSelectionDialog(
                                        "合成モードを選択:",
                                        "合成モード",
                                        listOf(
                                          "通常",
                                          "加算",
                                          "減算",
                                          "比較 (明)",
                                          "比較 (暗)",
                                          "色差",
                                          "除外",
                                          "乗算",
                                          "スクリーン",
                                          "オーバーレイ",
                                          "ハードライト",
                                          "ソフトライト",
                                          "覆い焼き",
                                          "焼き込み"
                                        )
                                      ) ?: return@onClick

                                    layer.blendMode = BlendModes.blendNameToBlendMode[newBlend] ?: PApplet.NORMAL

                                    handle.waitForLayerState++
                                  }
                                )
                              )
                            )
                          )
                        )
                      }

                      list
                    },
                    dev.wycey.mido.pui.renderer.layout.StackMainAxisAlignment.SpaceBetween
                  )
                )
              )
            ),
            crossAxisAlignment = dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment.Center
          )
        )
    )
  }
}
