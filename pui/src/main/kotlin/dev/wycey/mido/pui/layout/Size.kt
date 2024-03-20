package dev.wycey.mido.pui.layout

data class Size(val width: Float, val height: Float) {
  companion object {
    @JvmField
    val ZERO = Size(0f, 0f)

    @JvmStatic
    fun symmetric(size: Float) = Size(size, size)
  }

  fun contains(point: Point) = point.x in 0f..width && point.y in 0f..height

  fun toPoint() = Point(width, height)

  fun isFinite() = width.isFinite() && height.isFinite()

  operator fun compareTo(other: Size) = (width * height).compareTo(other.width * other.height)

  operator fun plus(other: Size) = Size(width + other.width, height + other.height)

  operator fun minus(other: Size) = Size(width - other.width, height - other.height)

  operator fun times(other: Size) = Size(width * other.width, height * other.height)

  operator fun div(other: Size) = Size(width / other.width, height / other.height)

  operator fun rem(other: Size) = Size(width % other.width, height % other.height)

  operator fun plus(other: Float) = Size(width + other, height + other)

  operator fun minus(other: Float) = Size(width - other, height - other)

  operator fun times(other: Float) = Size(width * other, height * other)

  operator fun div(other: Float) = Size(width / other, height / other)

  operator fun rem(other: Float) = Size(width % other, height % other)

  operator fun unaryMinus() = Size(-width, -height)
}
