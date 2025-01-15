package dev.wycey.mido.pui.state.signals.context

import dev.wycey.mido.pui.state.signals.Subscriber

public class RootSignalContext(
  rootSubscriber: Subscriber,
  private val disposeComputations: MutableList<() -> Unit> = mutableListOf()
) : SignalContext(rootSubscriber) {
  public fun dispose() {
    disposeComputations.forEach { it() }

    disposeComputations.clear()
  }

  public fun track(f: () -> Unit) {
    disposeComputations.add(f)
  }
}

public fun createRootSignalContext(rootSubscriber: Subscriber): RootSignalContext {
  val rootContext = RootSignalContext(rootSubscriber)

  SignalContext.rootContext = rootContext

  return rootContext
}

public fun nestRootSignalContextScope(
  context: RootSignalContext,
  f: () -> Unit
) {
  val previousRootContext = SignalContext.rootContext

  SignalContext.rootContext = context

  f()

  SignalContext.rootContext = previousRootContext
}

public fun runRootContext(
  context: RootSignalContext,
  f: () -> Unit
) {
  nestRootSignalContextScope(context) {
    f()
  }

  context.dispose()
}
