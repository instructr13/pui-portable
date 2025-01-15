package dev.wycey.mido.pui.components.basic

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.elements.basic.StatelessElement

public abstract class StatelessComponent
  @JvmOverloads
  constructor(key: String? = null) : Component(key) {
    public abstract fun build(context: BuildContext): Component

    override fun createElement() = StatelessElement(this)
  }
