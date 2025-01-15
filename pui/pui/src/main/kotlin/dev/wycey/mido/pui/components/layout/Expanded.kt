package dev.wycey.mido.pui.components.layout

import dev.wycey.mido.pui.components.base.Component

public class Expanded
  @JvmOverloads
  constructor(child: Component, flex: Int = 1, key: String? = null) :
  Flexible(child, flex, dev.wycey.mido.pui.renderer.layout.StackFit.Expand, key)
