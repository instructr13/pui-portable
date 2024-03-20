package dev.wycey.mido.pui.renderer

import dev.wycey.mido.pui.layout.constraints.lerp

data class BorderRadius(
  val topLeft: Float = 0f,
  val topRight: Float = 0f,
  val bottomRight: Float = 0f,
  val bottomLeft: Float = 0f
) {
  companion object {
    @JvmField
    val ZERO = BorderRadius()

    @JvmStatic
    fun all(radius: Float) = BorderRadius(radius, radius, radius, radius)

    @JvmStatic
    fun vertical(
      top: Float,
      bottom: Float
    ) = BorderRadius(top, top, bottom, bottom)

    @JvmStatic
    fun horizontal(
      left: Float,
      right: Float
    ) = BorderRadius(left, right, right, left)

    @JvmStatic
    fun lerp(
      a: BorderRadius,
      b: BorderRadius,
      t: Float
    ) = BorderRadius(
      a.topLeft.lerp(b.topLeft, t),
      a.topRight.lerp(b.topRight, t),
      a.bottomRight.lerp(b.bottomRight, t),
      a.bottomLeft.lerp(b.bottomLeft, t)
    )
  }

  operator fun plus(other: BorderRadius) =
    BorderRadius(
      topLeft + other.topLeft,
      topRight + other.topRight,
      bottomRight + other.bottomRight,
      bottomLeft + other.bottomLeft
    )

  operator fun minus(other: BorderRadius) =
    BorderRadius(
      topLeft - other.topLeft,
      topRight - other.topRight,
      bottomRight - other.bottomRight,
      bottomLeft - other.bottomLeft
    )

  operator fun unaryMinus() =
    BorderRadius(
      -topLeft,
      -topRight,
      -bottomRight,
      -bottomLeft
    )

  operator fun times(other: Float) =
    BorderRadius(
      topLeft * other,
      topRight * other,
      bottomRight * other,
      bottomLeft * other
    )

  operator fun div(other: Float) =
    BorderRadius(
      topLeft / other,
      topRight / other,
      bottomRight / other,
      bottomLeft / other
    )

  operator fun rem(other: Float) =
    BorderRadius(
      topLeft % other,
      topRight % other,
      bottomRight % other,
      bottomLeft % other
    )
}
