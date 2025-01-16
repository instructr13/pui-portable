package dev.wycey.mido.pui.util

import dev.wycey.mido.pui.layout.Point
import processing.core.PApplet

internal typealias ScopeConsumer = (Scope) -> Unit

public data class Scope(
  val applet: PApplet,
  val parent: Scope?
) {
  public var absolutePosition: Point = Point.ZERO
    private set
  public var relativePosition: Point = Point.ZERO
    private set

  public constructor(applet: PApplet) : this(applet, null)
  public constructor(parent: Scope) : this(parent.applet, parent)
  public constructor(parent: Scope, position: Point) : this(parent) {
    this.absolutePosition = position
  }

  public constructor(parent: Scope, position: Point, relativePosition: Point) : this(parent, position) {
    this.relativePosition = relativePosition
  }

  val rootScope: Scope
    get() {
      var scope = this

      while (scope.parent != null) {
        scope = scope.parent
      }

      return scope
    }

  public inline fun nestPositionalScope(
    position: Point,
    consumer: ScopeConsumer
  ) {
    applet.pushMatrix()

    applet.translate(position.x, position.y)

    consumer(Scope(this, this.absolutePosition + position, position))

    applet.popMatrix()
  }
}
