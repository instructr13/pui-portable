package dev.wycey.mido.pui.state.computation

import dev.wycey.mido.pui.state.signals.Signal
import dev.wycey.mido.pui.state.signals.Unsubscribe
import dev.wycey.mido.pui.state.signals.context.SignalContext
import dev.wycey.mido.pui.state.signals.data.ComputedSignalData

open class Computation<T : Any>(private val body: (previous: T?) -> T) {
  companion object {
    private val initialComputationStack = ArrayDeque<Computation<*>.InitialComputation>()

    @JvmStatic
    val lastComputation get() = initialComputationStack.lastOrNull()

    @JvmStatic
    fun pushComputation(initialComputation: Computation<*>.InitialComputation) =
      initialComputationStack.addLast(initialComputation)

    @JvmStatic
    fun popComputation() = initialComputationStack.removeLast()
  }

  private val scheduledSignals = mutableSetOf<Signal<*>>()
  private var lastValue: T
  private val disposeActions = mutableListOf<Unsubscribe>()
  private val updates = ComputationUpdatesStore()

  val pureSignals = mutableSetOf<Signal<*>>()

  inner class InitialComputation {
    fun <T> register(signal: Signal<T>) = scheduledSignals.add(signal)

    fun onDispose(f: Unsubscribe) = disposeActions.add(f)
  }

  init {
    val initialComputation = InitialComputation()

    pushComputation(initialComputation)

    lastValue = body(null)

    popComputation()

    scheduledSignals.forEach(::addSignalSources)
    scheduledSignals.clear()

    pureSignals.forEach { disposeActions.add(it.subscribe { onNotify(lastValue) }) }

    SignalContext.rootContext?.track(::dispose) ?: run {
      if (!SignalContext.warnedAboutRootContext) {
        println("Warning: no root context found, this computation will not be disposed")
        SignalContext.warnedAboutRootContext = true
      }
    }
  }

  private fun addSignalSources(signal: Signal<*>) {
    if (!signal.shouldSubscribe) {
      when (signal.parentSignalData) {
        is ComputedSignalData<*> -> {
          val computedSignal = (signal.parentSignalData as ComputedSignalData<*>).computedSignal

          computedSignal.sources.forEach(::addSignalSources)
        }
      }

      return
    }

    pureSignals.add(signal)
  }

  open fun onNotify(value: T) {
    lastValue = body(value)
  }

  fun dispose() {
    disposeActions.forEach { it() }

    pureSignals.apply {
      forEach { it.dispose() }

      clear()
    }
  }
}

inline fun createComputation(crossinline body: () -> Unit) = Computation<Unit> { body() }

inline fun createComputation(
  noinline body: () -> Unit,
  crossinline onNotify: () -> Unit
) = object : Computation<Unit>({ body() }) {
  override fun onNotify(value: Unit) = onNotify()
}

fun <T : Any> createComputation(body: (previous: T?) -> T) = Computation(body)

inline fun <T : Any> createComputation(
  noinline body: (previous: T?) -> T,
  crossinline onNotify: (value: T) -> Unit
) = object : Computation<T>(body) {
  override fun onNotify(value: T) = onNotify(value)
}
