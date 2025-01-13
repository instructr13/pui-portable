package dev.wycey.mido.pui.renderer.delegations

import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.data.BoxRendererData

interface ContainerRendererDataContract<ChildType : RendererObject> {
  var previousSibling: ChildType?
  var nextSibling: ChildType?
}

open class ContainerRendererData<ChildType : RendererObject> :
  BoxRendererData(),
  ContainerRendererDataContract<ChildType> {
  override var previousSibling: ChildType? = null
  override var nextSibling: ChildType? = null

  override fun toString(): String =
    "ContainerRendererParentData(previousSibling=$previousSibling, nextSibling=$nextSibling)"
}
