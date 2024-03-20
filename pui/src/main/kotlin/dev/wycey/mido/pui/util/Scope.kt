package dev.wycey.mido.pui.util

import dev.wycey.mido.pui.layout.Point
import processing.core.PApplet

typealias ScopeConsumer = (Scope) -> Unit

data class Scope(val applet: PApplet, val parent: Scope?) {
  var absolutePosition = Point.ZERO
    private set
  var relativePosition = Point.ZERO
    private set

  constructor(applet: PApplet) : this(applet, null)
  constructor(parent: Scope) : this(parent.applet, parent)
  constructor(parent: Scope, position: Point) : this(parent) {
    this.absolutePosition = position
  }

  constructor(parent: Scope, position: Point, relativePosition: Point) : this(parent, position) {
    this.relativePosition = relativePosition
  }

  val rootScope: Scope
    get() {
      var scope = this

      while (scope.parent != null) {
        scope = scope.parent!!
      }

      return scope
    }

  inline fun nestPositionalScope(
    position: Point,
    consumer: ScopeConsumer
  ) {
    applet.pushMatrix()

    applet.translate(position.x, position.y)

    consumer(Scope(this, this.absolutePosition + position, position))

    applet.popMatrix()
  }
}
