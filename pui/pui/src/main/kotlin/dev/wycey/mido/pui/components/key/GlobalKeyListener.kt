package dev.wycey.mido.pui.components.key

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.SingleChildRendererComponent
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.key.KeyEventArgs
import dev.wycey.mido.pui.renderer.RendererObject

class GlobalKeyListener
  @JvmOverloads
  constructor(
    child: Component,
    private val onKeyPress: ((e: KeyEventArgs) -> Unit)? = null,
    private val onKeyRelease: ((e: KeyEventArgs) -> Unit)? = null,
    private val onKeyType: ((e: KeyEventArgs) -> Unit)? = null,
    key: String? = null
  ) : SingleChildRendererComponent(key, child) {
    override fun createRenderer(context: BuildContext) =
      dev.wycey.mido.pui.renderer.key.GlobalKeyEventRenderer(onKeyPress, onKeyRelease, onKeyType)

    override fun updateRenderer(
      context: BuildContext,
      renderer: RendererObject
    ) {
      (renderer as dev.wycey.mido.pui.renderer.key.GlobalKeyEventRenderer).onKeyPress = onKeyPress
      renderer.onKeyRelease = onKeyRelease
      renderer.onKeyType = onKeyType
    }
  }
