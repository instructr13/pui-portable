package dev.wycey.mido.pui.state.signals.context

import dev.wycey.mido.pui.state.signals.Subscriber

public abstract class SignalContext(
  internal open val subscriber: Subscriber
) {
  public companion object {
    @JvmField
    public var rootContext: RootSignalContext? = null

    @JvmField
    internal var warnedAboutRootContext: Boolean = false
  }

  public open operator fun invoke() {
    subscriber()
  }
}
