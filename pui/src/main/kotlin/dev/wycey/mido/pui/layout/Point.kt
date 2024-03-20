package dev.wycey.mido.pui.layout

import kotlin.math.sqrt

data class Point(val x: Float, val y: Float) {
  companion object {
    @JvmField
    val ZERO = Point(0f, 0f)
  }

  constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

  fun distanceTo(point: Point): Float {
    val dx = x - point.x
    val dy = y - point.y

    return sqrt(dx * dx + dy * dy)
  }

  operator fun plus(other: Point) = Point(x + other.x, y + other.y)

  operator fun minus(other: Point) = Point(x - other.x, y - other.y)

  operator fun times(other: Point) = Point(x * other.x, y * other.y)

  operator fun div(other: Point) = Point(x / other.x, y / other.y)
}
