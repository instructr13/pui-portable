package dev.wycey.mido.pui.components.text

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.elements.base.BuildContext

class Text
  @JvmOverloads
  constructor(
    private val style: TextStyle = TextStyle(),
    key: String? = null,
    private val contentBuilder: () -> String
  ) : StatelessComponent(key) {
    @JvmOverloads
    constructor(
      content: String,
      style: TextStyle = TextStyle(),
      key: String? = null
    ) : this(style, key, { content })

    override fun build(context: BuildContext): Component = RawText(contentBuilder(), style)
  }
