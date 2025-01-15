package dev.wycey.mido.pui.elements.base

import dev.wycey.mido.pui.components.ComponentOwner
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.rendering.IndexedSlot
import dev.wycey.mido.pui.elements.rendering.RendererElement
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.RendererObject
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.util.Scope

internal object NullWidget : Component() {
  override fun createElement() = throw UnsupportedOperationException()
}

internal object NullElement : Element(NullWidget)

public abstract class Element(private var _component: Component?) : BuildContext, Comparable<Element> {
  override var component: Component
    get() = _component!!
    set(value) {
      _component = value
    }

  override var owner: ComponentOwner? = null

  override val mounted: Boolean
    get() = _component != null

  override val size: Size?
    get() {
      val renderer = findRenderer()

      if (renderer is BoxRenderer) {
        return renderer.size
      }

      return null
    }

  override val currentScope: Scope?
    get() {
      var current: Element? = this

      while (current != null) {
        if (current.currentScope != null) {
          return current.currentScope
        }

        current = current.parent
      }

      return null
    }

  public var parent: Element? = null

  public var slot: Any? = null
    protected set

  public open val renderer: RendererObject?
    get() {
      var current: Element? = this

      while (current != null) {
        if (current is RendererElement<*>) {
          return current.renderer
        }

        current = current.rendererAttachingChild
      }

      return null
    }

  public open val rendererAttachingChild: Element?
    get() {
      var next: Element? = null

      visitChildren {
        next = it
      }

      return next
    }

  public var depth: Int = 1
    private set

  public var dirty: Boolean = true
    private set

  internal var inDirtyList = false

  override fun findRenderer(): RendererObject? = renderer

  override fun visitAncestorElements(visitor: (element: Element) -> Boolean) {
    var ancestor = parent

    while (ancestor != null && visitor(ancestor)) {
      ancestor = ancestor.parent
    }
  }

  override fun visitChildElements(visitor: (element: Element) -> Unit): Unit = visitChildren(visitor)

  override fun compareTo(other: Element): Int {
    val diff = depth - other.depth

    if (diff != 0) return diff

    val isOtherDirty = other.dirty

    if (dirty != isOtherDirty) {
      return if (dirty) -1 else 1
    }

    return 0
  }

  public open fun visitChildren(visitor: (element: Element) -> Unit) {}

  protected open fun inflateComponent(
    newComponent: Component,
    newSlot: Any?
  ): Element {
    val newChild = newComponent.createElement()

    newChild.mount(this, newSlot)

    return newChild
  }

  protected fun updateChild(
    child: Element?,
    newComponent: Component?,
    newSlot: Any?
  ): Element? {
    if (newComponent == null) {
      if (child != null) {
        deactivateChild(child)
      }

      return null
    }

    val newChild: Element

    if (child != null) {
      if (child.component == newComponent) {
        if (child.slot != newSlot) {
          updateSlotForChild(child, newSlot)
        }

        newChild = child
      } else if (Component.canUpdate(child.component, newComponent)) {
        if (child.slot != newSlot) {
          updateSlotForChild(child, newSlot)
        }

        child.update(newComponent)

        newChild = child
      } else {
        deactivateChild(child)
        newChild = inflateComponent(newComponent, newSlot)
      }
    } else {
      newChild = inflateComponent(newComponent, newSlot)
    }

    return newChild
  }

  protected open fun updateSlot(newSlot: Any?) {
    slot = newSlot
  }

  protected fun updateSlotForChild(
    child: Element,
    newSlot: Any?
  ) {
    fun visit(element: Element) {
      element.updateSlot(newSlot)

      element.rendererAttachingChild?.let {
        visit(it)
      }
    }

    visit(child)
  }

  protected fun deactivateChild(child: Element) {
    child.parent = null
    child.detachRenderer()
    owner!!.inactiveElements.add(child)
  }

  protected fun mountChild(
    newComponent: Component,
    newSlot: Any?
  ): Element {
    val newChild = newComponent.createElement()

    newChild.mount(this, newSlot)

    return newChild
  }

  public open fun attachRenderer(newSlot: Any?) {
    visitChildren {
      it.attachRenderer(newSlot)
    }

    slot = newSlot
  }

  public open fun detachRenderer() {
    visitChildren {
      it.detachRenderer()
    }

    slot = null
  }

  public open fun update(newComponent: Component) {
    component = newComponent
  }

  protected open fun updateChildren(
    oldChildren: List<Element>,
    newComponents: List<Component>,
    slots: List<Any?>? = null
  ): MutableList<Element> {
    assert(slots == null || slots.size == newComponents.size)

    fun slotFor(
      newChildIndex: Int,
      previousChild: Element? = null
    ) = if (slots != null) {
      slots[newChildIndex]
    } else {
      IndexedSlot(
        newChildIndex,
        previousChild
      )
    }

    var newChildrenTop = 0
    var oldChildrenTop = 0
    var newChildrenBottom = newComponents.size - 1
    var oldChildrenBottom = oldChildren.size - 1

    val newChildren: MutableList<Element> =
      arrayOfNulls<Element>(newComponents.size).map { NullElement }.toMutableList()

    var previousChild: Element? = null

    while (newChildrenTop <= newChildrenBottom && oldChildrenTop <= oldChildrenBottom) {
      val newComponent = newComponents[newChildrenTop]
      val oldChild = oldChildren[oldChildrenTop]

      if (!Component.canUpdate(oldChild.component, newComponent)) {
        break
      }

      val newChild = updateChild(oldChild, newComponent, slotFor(newChildrenTop, previousChild))!!

      newChildren[newChildrenTop] = newChild
      previousChild = newChild

      newChildrenTop++
      oldChildrenTop++
    }

    while (oldChildrenTop <= oldChildrenBottom && newChildrenTop <= newChildrenBottom) {
      val newComponent = newComponents[newChildrenBottom]
      val oldChild = oldChildren[oldChildrenBottom]

      if (!Component.canUpdate(oldChild.component, newComponent)) {
        break
      }

      newChildrenBottom--
      oldChildrenBottom--
    }

    val haveOldChildren = oldChildrenTop <= oldChildrenBottom
    lateinit var oldKeyedChildren: MutableMap<String, Element>

    if (haveOldChildren) {
      oldKeyedChildren = mutableMapOf()

      while (oldChildrenTop <= oldChildrenBottom) {
        val oldChild = oldChildren[oldChildrenTop]

        if (oldChild.component.key != null) {
          oldKeyedChildren[oldChild.component.key!!] = oldChild
        } else {
          deactivateChild(oldChild)
        }

        oldChildrenTop++
      }
    }

    while (newChildrenTop <= newChildrenBottom) {
      var oldChild: Element? = null
      val newComponent = newComponents[newChildrenTop]

      if (haveOldChildren) {
        val key = newComponent.key

        if (key != null) {
          oldChild = oldKeyedChildren[key]

          if (oldChild != null) {
            if (Component.canUpdate(oldChild.component, newComponent)) {
              oldKeyedChildren.remove(key)
            } else {
              oldChild = null
            }
          }
        }
      }

      val newChild = updateChild(oldChild, newComponent, slotFor(newChildrenTop, previousChild))!!

      newChildren[newChildrenTop] = newChild
      previousChild = newChild

      newChildrenTop++
    }

    assert(newChildrenTop == newChildrenBottom + 1)
    assert(oldChildrenTop == oldChildrenBottom + 1)
    assert(newComponents.size - newChildrenTop == oldChildren.size - oldChildrenTop)

    newChildrenBottom = newChildren.size - 1
    oldChildrenBottom = oldChildren.size - 1

    while (oldChildrenTop <= oldChildrenBottom && newChildrenTop <= newChildrenBottom) {
      val oldChild = oldChildren[oldChildrenTop]
      val newComponent = newComponents[newChildrenTop]

      assert(Component.canUpdate(oldChild.component, newComponent))

      val newChild = updateChild(oldChild, newComponent, slotFor(newChildrenTop, previousChild))!!

      newChildren[newChildrenTop] = newChild
      previousChild = newChild

      newChildrenTop++
      oldChildrenTop++
    }

    if (haveOldChildren && oldKeyedChildren.isNotEmpty()) {
      for (oldChild in oldKeyedChildren.values) {
        deactivateChild(oldChild)
      }
    }

    return newChildren
  }

  override fun markAsDirty() {
    if (dirty) return

    dirty = true

    owner?.scheduleRebuild(this)
  }

  public open fun rebuild(force: Boolean = false) {
    if (!dirty && !force) return

    performRebuild()
  }

  public open fun mount(
    parent: Element?,
    newSlot: Any?
  ) {
    this.parent = parent
    slot = newSlot

    depth = parent?.depth?.plus(1) ?: 1

    if (parent != null) {
      owner = parent.owner
    }
  }

  public open fun unmount() {
    _component = null
  }

  public open fun needBuild() {
    markAsDirty()
  }

  public open fun performRebuild() {
    dirty = false
  }

  public open fun activate() {
    if (dirty) {
      owner!!.scheduleRebuild(this)
    }
  }

  public open fun deactivate() {}
}
