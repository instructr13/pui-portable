package dev.wycey.mido.pui.components.basic

import dev.wycey.mido.pui.components.base.Component

public abstract class ProxyComponent(internal val child: Component, key: String? = null) : Component(key)
