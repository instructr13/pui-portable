package dev.wycey.mido.pui.state.signals.context

import dev.wycey.mido.pui.state.signals.Subscriber

abstract class SignalContext(
  open val subscriber: Subscriber
) {
  companion object {
    @JvmField
    var rootContext: RootSignalContext? = null

    @JvmField
    var warnedAboutRootContext: Boolean = false
  }

  open operator fun invoke() {
    subscriber()
  }
}
