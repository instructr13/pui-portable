@file:JvmName("SignalCreator")

package dev.wycey.mido.pui.state.signals

import dev.wycey.mido.pui.state.computation.Computation
import dev.wycey.mido.pui.state.signals.context.SignalContext
import dev.wycey.mido.pui.state.signals.data.ParentSignalData
import dev.wycey.mido.pui.state.subscription.SubscriptionType
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal typealias Subscriber = () -> Unit
internal typealias Unsubscribe = () -> Unit
private typealias Getter<T> = () -> T
private typealias Setter<T> = (T) -> Unit

open class Signal<T>(var _value: T) : ReadWriteProperty<Any, T> {
  companion object {
    @JvmField
    val subscriptionCalls = ArrayDeque<SubscriptionType>()

    @JvmStatic
    val lastSubscriptionCall get() = subscriptionCalls.lastOrNull()

    @JvmStatic
    fun <T> valueOfSignal(signal: Signal<T>): T = signal._value
  }

  var parentSignalData: ParentSignalData? = null
  var shouldSubscribe = true

  private val subscribers = mutableSetOf<Subscriber>()

  fun subscribe(subscriber: Subscriber): Unsubscribe {
    subscribers.add(subscriber)

    return {
      unsubscribe(subscriber)
    }
  }

  private fun unsubscribe(subscriber: Subscriber) {
    subscribers.removeAll { s -> s == subscriber }
  }

  private fun getter(): Getter<T> =
    innerGetter@{
      if (lastSubscriptionCall is SubscriptionType.Untracked) {
        return@innerGetter _value
      }

      Computation.lastComputation?.register(this)

      if (SignalContext.rootContext != null) {
        val context = SignalContext.rootContext!!

        subscribers.add(context::invoke)

        context.track {
          unsubscribe(context::invoke)
        }
      }

      _value
    }

  operator fun getValue(
    thisRef: Nothing?,
    property: KProperty<*>
  ): T = getter()()

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>
  ): T = getter()()

  fun getValue(): T = getter()()

  private fun setter(): Setter<T> =
    innerSetter@{
      if (_value == it) return@innerSetter

      when (lastSubscriptionCall) {
        is SubscriptionType.Untracked -> {
          onNotify(it)

          return@innerSetter
        }

        is SubscriptionType.Batch ->
          lastSubscriptionCall?.let { maybeBatch -> maybeBatch as? SubscriptionType.Batch }?.apply {
            updates.add {
              _value = it
            }

            effects.add {
              onNotify(it)
            }

            effects.addAll(
              subscribers.map { subscriber ->
                subscriber
              }
            )

            return@innerSetter
          }

        else -> {
          // noop
        }
      }

      onNotify(it)

      subscribers.forEach { subscriber ->
        subscriber()
      }

      SignalContext.rootContext?.subscriber?.invoke()
    }

  operator fun setValue(
    thisRef: Nothing?,
    property: KProperty<*>,
    value: T
  ) = setter()(value)

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: T
  ) = setter()(value)

  fun setValue(value: T) = setter()(value)

  fun dispose() {
    subscribers.clear()
  }

  open fun onNotify(value: T) {
    this._value = value
  }

  override fun toString(): String {
    return "Signal(value=$_value)"
  }
}

inline fun <reified T> signal(value: T) = Signal(value)
