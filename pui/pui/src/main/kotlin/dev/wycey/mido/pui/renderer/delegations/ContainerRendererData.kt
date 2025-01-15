package dev.wycey.mido.pui.renderer.delegations

import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.data.BoxRendererData

public interface ContainerRendererDataContract<ChildType : RendererObject> {
  public var previousSibling: ChildType?
  public var nextSibling: ChildType?
}

public open class ContainerRendererData<ChildType : RendererObject> :
  BoxRendererData(),
  ContainerRendererDataContract<ChildType> {
  override var previousSibling: ChildType? = null
  override var nextSibling: ChildType? = null

  override fun toString(): String =
    "ContainerRendererParentData(previousSibling=$previousSibling, nextSibling=$nextSibling)"
}
