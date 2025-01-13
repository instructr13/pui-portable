package dev.wycey.mido.pui.state.signals.data

import dev.wycey.mido.pui.state.signals.ComputedSignal

data class ComputedSignalData<T>(val computedSignal: ComputedSignal<T>) : ParentSignalData()
