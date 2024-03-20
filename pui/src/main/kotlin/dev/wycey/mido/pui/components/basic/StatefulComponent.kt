package dev.wycey.mido.pui.components.basic

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.basic.StatefulElement
import dev.wycey.mido.pui.state.signals.ComputedSignal
import dev.wycey.mido.pui.state.signals.Signal
import dev.wycey.mido.pui.state.signals.context.RootSignalContext
import dev.wycey.mido.pui.state.signals.context.nestRootSignalContextScope
import dev.wycey.mido.pui.state.signals.effect as _effect

abstract class StatefulComponent
  @JvmOverloads
  constructor(key: String? = null) : Component(key) {
    open inner class State {
      private val onBuildCallbacks = mutableListOf<() -> Unit>()

      var firstBuild = true

      private var signalIndex = 0
      private val createdSignals = mutableListOf<Signal<*>>()

      private var computedIndex = 0
      private val createdComputedSignals = mutableListOf<ComputedSignal<*>>()

      private var effectIndex = 0
      private val createdEffects = mutableListOf<Function<*>>()

      private var functionIndex = 0
      private val createdFunctions = mutableListOf<Function<*>>()

      var _element: StatefulElement? = null
      var _component: Component? = null
      val context =
        RootSignalContext({
          if (!firstBuild) {
            _element!!.markAsDirty()
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

      fun effect(f: () -> Unit) =
        if (firstBuild) {
          _effect(f).also { createdEffects.add(f) }
        } else {
          createdEffects.getOrElse(effectIndex++) {
            throw IllegalStateException("Effect index out of bounds")
          }
        }

      fun <T : Any> effect(subscriber: (previous: T?) -> T) =
        if (firstBuild) {
          _effect(subscriber).also { createdEffects.add(subscriber) }
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

    var state: State? = null

    fun createState() = State()

    fun <T> signal(value: T) = state!!.signal(value)

    fun <T> computed(compute: () -> T) = state!!.computed(compute)

    fun effect(f: () -> Unit) = state!!.effect(f)

    fun <T : Any> effect(subscriber: (previous: T?) -> T) = state!!.effect(subscriber)

    fun <T : Any> createFunction(fn: () -> T) = state!!.createFunction(fn)

    fun onRender(callback: () -> Unit) = state!!.onRender(callback)

    abstract fun build(context: BuildContext): Component

    override fun createElement() = StatefulElement(this)
  }
