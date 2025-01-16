package dev.wycey.mido.pui.components.basic

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.basic.StatefulElement
import dev.wycey.mido.pui.state.signals.ComputedSignal
import dev.wycey.mido.pui.state.signals.Signal
import dev.wycey.mido.pui.state.signals.Unsubscribe
import dev.wycey.mido.pui.state.signals.context.RootSignalContext
import dev.wycey.mido.pui.state.signals.context.nestRootSignalContextScope
import dev.wycey.mido.pui.state.signals.effect as _effect

public abstract class StatefulComponent
  @JvmOverloads
  constructor(
    key: String? = null
  ) : Component(key) {
    internal open inner class State {
      private val onBuildCallbacks = mutableListOf<() -> Unit>()

      var firstBuild = true

      private var signalIndex = 0
      private val createdSignals = mutableListOf<Signal<*>>()

      private var computedIndex = 0
      private val createdComputedSignals = mutableListOf<ComputedSignal<*>>()

      private var effectIndex = 0
      private val createdEffects = mutableListOf<Unsubscribe>()

      private var functionIndex = 0
      private val createdFunctions = mutableListOf<() -> Any>()

      var linkedElement: StatefulElement? = null
      var component: Component? = null

      val context =
        RootSignalContext({
          if (!firstBuild) {
            linkedElement!!.markAsDirty()
          }
        })

      fun <T> signal(value: T) =
        if (firstBuild) {
          Signal(value).also { createdSignals.add(it) }
        } else {
          createdSignals.getOrElse(signalIndex++) {
            throw IllegalStateException("Signal index out of bounds")
          } as Signal<T>
        }

      fun <T> computed(compute: () -> T) =
        if (firstBuild) {
          ComputedSignal(compute).also { createdComputedSignals.add(it) }
        } else {
          createdComputedSignals.getOrElse(computedIndex++) {
            throw IllegalStateException("Computed signal index out of bounds")
          } as ComputedSignal<T>
        }

      fun effect(subscriber: () -> Unit) =
        if (firstBuild) {
          _effect(subscriber).apply { createdEffects.add(this) }
        } else {
          createdEffects.getOrElse(effectIndex++) {
            throw IllegalStateException("Effect index out of bounds")
          }
        }

      fun <T : Any> effect(subscriber: (previous: T?) -> T) =
        if (firstBuild) {
          _effect(subscriber).apply { createdEffects.add(this) }
        } else {
          createdEffects.getOrElse(effectIndex++) {
            throw IllegalStateException("Effect index out of bounds")
          }
        }

      fun <T : Any> createFunction(fn: () -> T) =
        if (firstBuild) {
          fn.also { createdFunctions.add(fn) }
        } else {
          createdFunctions.getOrElse(functionIndex++) {
            throw IllegalStateException("Function index out of bounds")
          } as () -> T
        }

      fun onRender(callback: () -> Unit) {
        if (firstBuild) {
          onBuildCallbacks.add(callback)
        }
      }

      fun resetIndex() {
        signalIndex = 0
        computedIndex = 0
        effectIndex = 0
        functionIndex = 0
      }

      fun emitBuild() {
        onBuildCallbacks.forEach { it() }
      }

      open fun activate() {}

      open fun deactivate() {}

      fun build(context: BuildContext): Component {
        var component: Component? = null

        nestRootSignalContextScope(this.context) {
          component = this@StatefulComponent.build(context)
        }

        return component!!
      }

      fun dispose() {
        onBuildCallbacks.clear()
        createdEffects.clear()
        createdComputedSignals.clear()
        createdSignals.forEach { it.dispose() }
        createdSignals.clear()

        context.dispose()
      }
    }

    internal var state: State? = null

    internal fun createState() = State()

    public fun <T> signal(value: T): Signal<T> = state!!.signal(value)

    public fun <T> computed(compute: () -> T): ComputedSignal<T> = state!!.computed(compute)

    public fun effect(f: () -> Unit): Unsubscribe = state!!.effect(f)

    public fun <T : Any> effect(subscriber: (previous: T?) -> T): Unsubscribe = state!!.effect(subscriber)

    public fun <T : Any> createFunction(fn: () -> T): () -> T = state!!.createFunction(fn)

    public fun onRender(callback: () -> Unit): Unit = state!!.onRender(callback)

    public abstract fun build(context: BuildContext): Component

    override fun createElement(): StatefulElement = StatefulElement(this)
  }
