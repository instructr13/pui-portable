package dev.wycey.mido.pui.elements.root

import dev.wycey.mido.pui.bridges.RendererBridge
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.root.ViewComponent
import dev.wycey.mido.pui.elements.base.Element
import dev.wycey.mido.pui.elements.rendering.RendererElement
import dev.wycey.mido.pui.renderer.RenderGlobalContext
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.renderer.view.ViewRenderer

class ViewElement(view: ViewComponent.InnerViewComponent) : RendererElement<BoxRenderer>(view) {
  var _parentContext: RenderGlobalContext? = null
  var child: Element? = null
  val context = RenderGlobalContext()

  override val renderer: ViewRenderer
    get() = super.renderer as ViewRenderer

  override fun attachRenderer(newSlot: Any?) {
    slot = newSlot
  }

  override fun detachRenderer() {
    slot = null
  }

  private fun attachView(parentContext: RenderGlobalContext? = null) {
    val nonNullParentContext = parentContext ?: RendererBridge.rootContext

    nonNullParentContext.adoptChild(context)

    RendererBridge.instance.addRenderView(renderer)
  }

  private fun detachView() {
    val parentContext = _parentContext

    if (parentContext != null) {
      RendererBridge.instance.removeRenderView(renderer)

      parentContext.dropChild(context)

      _parentContext = null
    }
  }

  override fun needBuild() {
    super.needBuild()

    if (_parentContext == null) return

    val newContext = RendererBridge.rootContext

    if (newContext != _parentContext) {
      detachView()
      attachView(newContext)
    }
  }

  private fun _updateChild() {
    val builtChild = (component as ViewComponent.InnerViewComponent).builder()

    child = updateChild(child, builtChild, null)
  }

  override fun performRebuild() {
    super.performRebuild()

    _updateChild()
  }

  override fun activate() {
    super.activate()

    context.rootNode = renderer

    attachView()
  }

  override fun deactivate() {
    detachView()

    context.rootNode = null

    super.deactivate()
  }

  override fun update(newComponent: Component) {
    super.update(newComponent)

    _updateChild()
  }

  override fun visitChildren(visitor: (element: Element) -> Unit) {
    if (child != null) {
      visitor(child!!)
    }
  }

  override fun mount(
    parent: Element?,
    newSlot: Any?
  ) {
    super.mount(parent, newSlot)

    context.rootNode = renderer

    attachView()
    _updateChild()
    renderer.prepareInitialFrame()

    println("View element mounted")
  }

  override fun insertRendererChild(
    child: BoxRenderer,
    slot: Any?
  ) {
    renderer.child = child
  }

  override fun moveRendererChild(
    child: BoxRenderer,
    oldSlot: Any?,
    newSlot: Any?
  ) = throw UnsupportedOperationException("Cannot move child of ViewElement")

  override fun removeRendererChild(
    child: BoxRenderer,
    slot: Any?
  ) {
    renderer.child = null
  }
}
