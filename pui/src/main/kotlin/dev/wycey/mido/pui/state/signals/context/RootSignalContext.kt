package dev.wycey.mido.pui.state.signals.context

import dev.wycey.mido.pui.state.signals.Subscriber

class RootSignalContext(
  rootSubscriber: Subscriber,
  private val disposeComputations: MutableList<() -> Unit> = mutableListOf()
) : SignalContext(rootSubscriber) {
  fun dispose() {
    disposeComputations.forEach { it() }

    disposeComputations.clear()
  }

  fun track(f: () -> Unit) {
    disposeComputations.add(f)
  }
}

fun createRootSignalContext(rootSubscriber: Subscriber): RootSignalContext {
  val rootContext = RootSignalContext(rootSubscriber)

  SignalContext.rootContext = rootContext

  return rootContext
}

fun nestRootSignalContextScope(
  context: RootSignalContext,
  f: () -> Unit
) {
  val previousRootContext = SignalContext.rootContext

  SignalContext.rootContext = context

  f()

  SignalContext.rootContext = previousRootContext
}

fun runRootContext(
  context: RootSignalContext,
  f: () -> Unit
) {
  nestRootSignalContextScope(context) {
    f()
  }

  context.dispose()
}
