package dev.wycey.mido.pui.elements.base

import dev.wycey.mido.pui.components.ComponentOwner
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.util.Scope

public interface BuildContext {
  public var component: Component
  public var owner: ComponentOwner?
  public val mounted: Boolean
  public val size: Size?
  public val currentScope: Scope?

  public fun findRenderer(): RendererObject?

  public fun visitAncestorElements(visitor: (element: Element) -> Boolean)

  public fun visitChildElements(visitor: (element: Element) -> Unit)

  public fun markAsDirty()
}
