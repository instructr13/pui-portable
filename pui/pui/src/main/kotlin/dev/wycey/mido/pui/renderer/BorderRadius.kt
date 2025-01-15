package dev.wycey.mido.pui.renderer

import dev.wycey.mido.pui.layout.constraints.lerp

public data class BorderRadius(
  val topLeft: Float = 0f,
  val topRight: Float = 0f,
  val bottomRight: Float = 0f,
  val bottomLeft: Float = 0f
) {
  public companion object {
    @JvmField
    public val ZERO: BorderRadius = BorderRadius()

    @JvmStatic
    public fun all(radius: Float): BorderRadius = BorderRadius(radius, radius, radius, radius)

    @JvmStatic
    public fun vertical(
      top: Float,
      bottom: Float
    ): BorderRadius = BorderRadius(top, top, bottom, bottom)

    @JvmStatic
    public fun horizontal(
      left: Float,
      right: Float
    ): BorderRadius = BorderRadius(left, right, right, left)

    @JvmStatic
    public fun lerp(
      a: BorderRadius,
      b: BorderRadius,
      t: Float
    ): BorderRadius =
      BorderRadius(
        a.topLeft.lerp(b.topLeft, t),
        a.topRight.lerp(b.topRight, t),
        a.bottomRight.lerp(b.bottomRight, t),
        a.bottomLeft.lerp(b.bottomLeft, t)
      )
  }

  public operator fun plus(other: BorderRadius): BorderRadius =
    BorderRadius(
      topLeft + other.topLeft,
      topRight + other.topRight,
      bottomRight + other.bottomRight,
      bottomLeft + other.bottomLeft
    )

  public operator fun minus(other: BorderRadius): BorderRadius =
    BorderRadius(
      topLeft - other.topLeft,
      topRight - other.topRight,
      bottomRight - other.bottomRight,
      bottomLeft - other.bottomLeft
    )

  public operator fun unaryMinus(): BorderRadius =
    BorderRadius(
      -topLeft,
      -topRight,
      -bottomRight,
      -bottomLeft
    )

  public operator fun times(other: Float): BorderRadius =
    BorderRadius(
      topLeft * other,
      topRight * other,
      bottomRight * other,
      bottomLeft * other
    )

  public operator fun div(other: Float): BorderRadius =
    BorderRadius(
      topLeft / other,
      topRight / other,
      bottomRight / other,
      bottomLeft / other
    )

  public operator fun rem(other: Float): BorderRadius =
    BorderRadius(
      topLeft % other,
      topRight % other,
      bottomRight % other,
      bottomLeft % other
    )
}
