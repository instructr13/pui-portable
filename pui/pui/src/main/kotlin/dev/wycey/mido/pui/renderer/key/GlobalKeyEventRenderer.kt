package dev.wycey.mido.pui.renderer.key

import dev.wycey.mido.pui.bridges.EventHandlingBridge
import dev.wycey.mido.pui.events.key.KeyEventArgs
import dev.wycey.mido.pui.events.key.KeyEventType
import dev.wycey.mido.pui.renderer.box.ProxyBoxRenderer

public class GlobalKeyEventRenderer(
  public var onKeyPress: ((e: KeyEventArgs) -> Unit)? = null,
  public var onKeyRelease: ((e: KeyEventArgs) -> Unit)? = null,
  public var onKeyType: ((e: KeyEventArgs) -> Unit)? = null
) : ProxyBoxRenderer() {
  private val fn: (KeyEventArgs, KeyEventType) -> Unit = { e, type ->
    when (type) {
      KeyEventType.Press -> onKeyPress?.invoke(e)
      KeyEventType.Release -> onKeyRelease?.invoke(e)
      KeyEventType.Type -> onKeyType?.invoke(e)
    }
  }

  override fun performInsertChild() {
    EventHandlingBridge.instance.addGlobalKeyEvent(fn)
  }

  override fun performDrop() {
    EventHandlingBridge.instance.removeGlobalKeyEvent(fn)
  }
}
