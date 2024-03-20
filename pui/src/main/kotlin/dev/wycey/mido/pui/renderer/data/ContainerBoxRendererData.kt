package dev.wycey.mido.pui.renderer.data

import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererData
import dev.wycey.mido.pui.renderer.delegations.ContainerRendererDataContract

class ContainerBoxRendererData<ChildType : RendererObject> :
  BoxRendererData(),
  ContainerRendererDataContract<ChildType> by ContainerRendererData()
