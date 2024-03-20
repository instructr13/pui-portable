package dev.wycey.mido.pui.util

import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LazyOnce<T> : ReadWriteProperty<Any, T> {
  private val valueRef = AtomicReference<T>()

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>
  ): T = valueRef.get() ?: throw IllegalAccessError("Value isn't initialized")

  operator fun getValue(
    nullThisRef: Nothing?,
    property: KProperty<*>
  ): T = valueRef.get() ?: throw IllegalAccessError("Value isn't initialized")

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: T
  ) {
    if (!valueRef.compareAndSet(null, value)) {
      throw IllegalAccessError("Value is initialized")
    }
  }

  operator fun setValue(
    nullThisRef: Nothing?,
    property: KProperty<*>,
    value: T
  ) {
    if (!valueRef.compareAndSet(null, value)) {
      throw IllegalAccessError("Value is initialized")
    }
  }
}

inline fun <reified T> lazyOnce() = LazyOnce<T>()
