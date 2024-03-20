package dev.wycey.mido.pui.components.basic

import dev.wycey.mido.pui.components.base.Component

abstract class ProxyComponent(val child: Component, key: String? = null) : Component(key)
