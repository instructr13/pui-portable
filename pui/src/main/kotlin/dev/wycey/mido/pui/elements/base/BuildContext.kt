package dev.wycey.mido.pui.elements.base

import dev.wycey.mido.pui.components.ComponentOwner
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.util.Scope

interface BuildContext {
  var component: Component
  var owner: ComponentOwner?
  val mounted: Boolean
  val size: Size?
  val currentScope: Scope?

  fun findRenderer(): RendererObject?

  fun visitAncestorElements(visitor: (element: Element) -> Boolean)

  fun visitChildElements(visitor: (element: Element) -> Unit)

  fun markAsDirty()
}
