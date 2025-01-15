package dev.wycey.mido.pui.bridges

import dev.wycey.mido.pui.components.ComponentOwner
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.root.RootComponent
import dev.wycey.mido.pui.components.root.ViewComponent
import dev.wycey.mido.pui.elements.base.Element
import dev.wycey.mido.pui.elements.root.RootElement

internal interface ComponentsBridgeContract {
  fun drawFrame()

  fun attachRootComponent(component: Component)

  fun attachToComponentOwner(component: RootComponent)

  fun wrapWithProcessingView(rootComponent: Component): Component
}

public class ComponentsBridge internal constructor(
  private val mouseEventBridge: MouseEventBridge,
  private val frameEventBridge: FrameEventBridge,
  private val rendererBridge: RendererBridge
) : ComponentsBridgeContract,
  MouseEventBridgeContract by mouseEventBridge,
  FrameEventBridgeContract by frameEventBridge,
  RendererBridgeContract by rendererBridge,
  BridgeBase() {
  public companion object {
    @JvmField
    internal var instanceNullable: ComponentsBridge? = null

    @JvmStatic
    public val instance: ComponentsBridge get() = checkInstance(instanceNullable)
  }

  private var rootElement: Element? = null

  private var componentOwner: ComponentOwner? = null

  override fun initInstance() {
    rendererBridge.initInstance()
    instanceNullable = this

    componentOwner = ComponentOwner()

    println("Components bridge initialized")
  }

  override fun drawFrame() {
    if (rootElement != null) {
      // componentOwner!!.buildScope(rootElement!!)
      componentOwner!!.buildScope()
    }

    rendererBridge.drawFrame()
    componentOwner!!.finalizeFrame()
  }

  override fun attachRootComponent(component: Component) {
    attachToComponentOwner(
      RootComponent(
        component
      )
    )
  }

  override fun attachToComponentOwner(component: RootComponent) {
    rootElement = component.attach(componentOwner!!, rootElement as RootElement?)
  }

  override fun wrapWithProcessingView(rootComponent: Component): Component =
    ViewComponent(
      rootComponent
    )
}
