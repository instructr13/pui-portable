package dev.wycey.mido.pui.components

import dev.wycey.mido.pui.elements.base.Element

public class ComponentOwner {
  private val dirtyElements = mutableListOf<Element>()
  private var scheduledFlushDirtyComponents = false
  private var dirtyElementsNeedsResorting: Boolean? = null
  internal val inactiveElements = mutableListOf<Element>()

  internal fun scheduleRebuild(element: Element) {
    if (element.inDirtyList) {
      dirtyElementsNeedsResorting = true

      return
    }

    dirtyElements.add(element)
    element.inDirtyList = true
  }

  internal fun buildScope(f: (() -> Unit)? = null) {
    if (f == null && dirtyElements.isEmpty()) return

    scheduledFlushDirtyComponents = true

    if (f != null) f()

    dirtyElements.sort()
    dirtyElementsNeedsResorting = false

    var dirtyCount = dirtyElements.size
    var index = 0

    while (index < dirtyCount) {
      val element = dirtyElements[index]

      element.rebuild()

      index++

      if (dirtyCount < dirtyElements.size || dirtyElementsNeedsResorting!!) {
        dirtyElements.sort()
        dirtyElementsNeedsResorting = false

        dirtyCount = dirtyElements.size

        while (index > 0 && dirtyElements[index - 1].dirty) {
          index--
        }
      }
    }

    dirtyElements.forEach {
      it.inDirtyList = false
    }

    dirtyElements.clear()

    scheduledFlushDirtyComponents = false
    dirtyElementsNeedsResorting = null
  }

  internal fun finalizeFrame() {
    fun unmount(element: Element) {
      element.visitChildren {
        unmount(it)
      }

      element.unmount()
    }

    inactiveElements.reversed().forEach(::unmount)
    inactiveElements.clear()
  }
}
