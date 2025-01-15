package dev.wycey.mido.pui.layout

public data class Size(val width: Float, val height: Float) {
  public companion object {
    @JvmField
    public val ZERO: Size = Size(0f, 0f)

    @JvmStatic
    public fun symmetric(size: Float): Size = Size(size, size)
  }

  public fun contains(point: Point): Boolean = point.x in 0f..width && point.y in 0f..height

  public fun toPoint(): Point = Point(width, height)

  public fun isFinite(): Boolean = width.isFinite() && height.isFinite()

  public operator fun compareTo(other: Size): Int = (width * height).compareTo(other.width * other.height)

  public operator fun plus(other: Size): Size = Size(width + other.width, height + other.height)

  public operator fun minus(other: Size): Size = Size(width - other.width, height - other.height)

  public operator fun times(other: Size): Size = Size(width * other.width, height * other.height)

  public operator fun div(other: Size): Size = Size(width / other.width, height / other.height)

  public operator fun rem(other: Size): Size = Size(width % other.width, height % other.height)

  public operator fun plus(other: Float): Size = Size(width + other, height + other)

  public operator fun minus(other: Float): Size = Size(width - other, height - other)

  public operator fun times(other: Float): Size = Size(width * other, height * other)

  public operator fun div(other: Float): Size = Size(width / other, height / other)

  public operator fun rem(other: Float): Size = Size(width % other, height % other)

  public operator fun unaryMinus(): Size = Size(-width, -height)
}
