package dev.wycey.mido.pui.components.rendering

import dev.wycey.mido.pui.elements.base.Element

public data class IndexedSlot<T : Element?>(val index: Int, val value: T)
