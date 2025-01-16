package dev.wycey.mido.pui.layout

import kotlin.math.sqrt

public data class Point(
  val x: Float,
  val y: Float
) {
  public companion object {
    @JvmField
    public val ZERO: Point = Point(0f, 0f)
  }

  public constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

  public fun distanceTo(point: Point): Float {
    val dx = x - point.x
    val dy = y - point.y

    return sqrt(dx * dx + dy * dy)
  }

  public operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)

  public operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)

  public operator fun times(other: Point): Point = Point(x * other.x, y * other.y)

  public operator fun div(other: Point): Point = Point(x / other.x, y / other.y)
}
