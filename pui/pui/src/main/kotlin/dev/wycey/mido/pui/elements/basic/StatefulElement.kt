package dev.wycey.mido.pui.elements.basic

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent

public class StatefulElement(
  component: StatefulComponent
) : ComponentElement(component) {
  private var nullableState: StatefulComponent.State? = component.createState()
  internal val state get(): StatefulComponent.State = nullableState!!

  private var rebuilt = false

  init {
    state.linkedElement = this
    state.component = component

    component.state = state
  }

  override fun build(): Component {
    state.resetIndex()

    return state.build(this)
  }

  override fun firstBuild() {
    super.firstBuild()

    state.firstBuild = false

    state.emitBuild()
  }

  override fun performRebuild() {
    if (rebuilt) {
      state.emitBuild()
      rebuilt = false
    }

    super.performRebuild()
  }

  override fun update(newComponent: Component) {
    super.update(newComponent)

    state.component = newComponent as StatefulComponent

    rebuild(true)
  }

  override fun activate() {
    super.activate()
    state.activate()

    markAsDirty()
  }

  override fun deactivate() {
    super.deactivate()
    state.deactivate()

    markAsDirty()
  }

  override fun unmount() {
    super.unmount()

    state.dispose()

    state.linkedElement = null

    nullableState = null
  }

  override fun needBuild() {
    super.needBuild()

    rebuilt = true
  }
}
