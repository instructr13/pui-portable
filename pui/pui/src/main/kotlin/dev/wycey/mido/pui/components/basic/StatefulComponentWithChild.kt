package dev.wycey.mido.pui.components.basic

import dev.wycey.mido.pui.components.base.Component

abstract class StatefulComponentWithChild
  @JvmOverloads
  protected constructor(
    key: String?,
    protected val childBuilder: (() -> Component)? = null
  ) : StatefulComponent(key) {
    protected constructor(key: String?, child: Component) : this(key, { child })
  }
