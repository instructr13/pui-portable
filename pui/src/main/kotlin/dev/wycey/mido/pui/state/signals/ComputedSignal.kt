@file:JvmName("Computed")

package dev.wycey.mido.pui.state.signals

import dev.wycey.mido.pui.state.computation.createComputation
import dev.wycey.mido.pui.state.signals.data.ComputedSignalData
import dev.wycey.mido.pui.state.subscription.SubscriptionType
import dev.wycey.mido.pui.state.subscription.runWithSubscriptionCallStack
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ComputedSignal<T>(private val f: () -> T) : ReadOnlyProperty<Any, T> {
  private var signal: Signal<T?> =
    Signal(null).run {
      shouldSubscribe = false
      parentSignalData = ComputedSignalData(this@ComputedSignal)

      this as Signal<T?>
    }

  private val computation =
    createComputation({ signal._value = f() }) {
      runWithSubscriptionCallStack(SubscriptionType.Untracked) { signal._value = f() }
    }

  val sources get() = computation.pureSignals

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>
  ): T = signal.getValue(thisRef, property)!!

  operator fun getValue(
    nullThisRef: Nothing?,
    property: KProperty<*>
  ): T = signal.getValue(nullThisRef, property)!!

  fun getValue(): T = signal.getValue()!!
}

inline fun <reified T> computed(noinline f: () -> T) = ComputedSignal(f)
