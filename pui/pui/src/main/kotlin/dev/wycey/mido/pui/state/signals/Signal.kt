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

public open class Signal<T>(
  internal var innerValue: T
) : ReadWriteProperty<Any, T> {
  internal companion object {
    @JvmField
    val subscriptionCalls = ArrayDeque<SubscriptionType>()

    @JvmStatic
    val lastSubscriptionCall get() = subscriptionCalls.lastOrNull()

    @JvmStatic
    fun <T> valueOfSignal(signal: Signal<T>): T = signal.innerValue
  }

  internal var parentSignalData: ParentSignalData? = null
  internal var shouldSubscribe = true

  private val subscribers = mutableSetOf<Subscriber>()

  internal fun subscribe(subscriber: Subscriber): Unsubscribe {
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
        return@innerGetter innerValue
      }

      Computation.lastComputation?.register(this)

      if (SignalContext.rootContext != null) {
        val context = SignalContext.rootContext!!

        subscribers.add(context::invoke)

        context.track {
          unsubscribe(context::invoke)
        }
      }

      innerValue
    }

  @Suppress("unused")
  public operator fun getValue(
    thisRef: Nothing?,
    property: KProperty<*>
  ): T = getter()()

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>
  ): T = getter()()

  public fun getValue(): T = getter()()

  private fun setter(): Setter<T> =
    innerSetter@{
      if (innerValue == it) return@innerSetter

      when (lastSubscriptionCall) {
        is SubscriptionType.Untracked -> {
          onNotify(it)

          return@innerSetter
        }

        is SubscriptionType.Batch ->
          lastSubscriptionCall?.let { maybeBatch -> maybeBatch as? SubscriptionType.Batch }?.apply {
            updates.add {
              innerValue = it
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

  @Suppress("unused")
  public operator fun setValue(
    thisRef: Nothing?,
    property: KProperty<*>,
    value: T
  ): Unit = setter()(value)

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: T
  ): Unit = setter()(value)

  public fun setValue(value: T): Unit = setter()(value)

  public fun dispose() {
    subscribers.clear()
  }

  internal open fun onNotify(value: T) {
    this.innerValue = value
  }

  override fun toString(): String = "Signal(value=$innerValue)"
}

public inline fun <reified T> signal(value: T): Signal<T> = Signal(value)
