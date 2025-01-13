package dev.wycey.mido.pui.layout

import dev.wycey.mido.pui.layout.constraints.lerp

abstract class AlignmentFactor {
  companion object {
    @JvmStatic
    fun lerp(
      a: AlignmentFactor?,
      b: AlignmentFactor?,
      t: Float
    ): AlignmentFactor? {
      if (a == b) return a

      if (a == null) return b!! * t
      if (b == null) return a * (1f - t)

      if (a is Alignment && b is Alignment) return Alignment.lerp(a, b, t)

      if (a is AlignmentDirectional && b is AlignmentDirectional) return AlignmentDirectional.lerp(a, b, t)

      return MixedAlignment(
        a.x.lerp(b.x, t),
        a.start.lerp(b.start, t),
        a.y.lerp(b.y, t)
      )
    }
  }

  private data class MixedAlignment(override val x: Float, override val start: Float, override val y: Float) :
    AlignmentFactor() {
    override fun unaryMinus() = MixedAlignment(-x, -start, -y)

    override fun plus(other: AlignmentFactor) = MixedAlignment(x + other.x, start + other.start, y + other.y)

    override fun times(other: Float) = MixedAlignment(x * other, start * other, y * other)

    override fun div(other: Float) = MixedAlignment(x / other, start / other, y / other)

    override fun rem(other: Float) = MixedAlignment(x % other, start % other, y % other)

    override fun resolve() = Alignment(x + start, y)
  }

  abstract val x: Float
  abstract val start: Float
  abstract val y: Float

  abstract operator fun unaryMinus(): AlignmentFactor

  abstract operator fun plus(other: AlignmentFactor): AlignmentFactor

  abstract operator fun times(other: Float): AlignmentFactor

  abstract operator fun div(other: Float): AlignmentFactor

  abstract operator fun rem(other: Float): AlignmentFactor

  fun alongOffset(offset: Point): Point {
    val centerX = offset.x / 2
    val centerY = offset.y / 2

    return Point(
      centerX + x * centerX + start * offset.x,
      centerY + y * centerY
    )
  }

  override fun equals(other: Any?) = other is AlignmentFactor && x == other.x && start == other.start && y == other.y

  override fun hashCode(): Int {
    var result = x.hashCode()
    result = 31 * result + start.hashCode()
    result = 31 * result + y.hashCode()
    return result
  }

  abstract fun resolve(): Alignment
}

class Alignment(override val x: Float, override val y: Float) : AlignmentFactor() {
  companion object {
    @JvmStatic
    fun lerp(
      a: Alignment?,
      b: Alignment?,
      t: Float
    ): Alignment? {
      if (a == b) return a

      if (a == null) return Alignment(0f.lerp(b!!.x, t), 0f.lerp(b.y, t))
      if (b == null) return Alignment(a.x.lerp(0f, t), a.y.lerp(0f, t))

      return Alignment(a.x.lerp(b.x, t), a.y.lerp(b.y, t))
    }

    @JvmField
    val topLeft = Alignment(-1f, -1f)

    @JvmField
    val topCenter = Alignment(0f, -1f)

    @JvmField
    val topRight = Alignment(1f, -1f)

    @JvmField
    val centerLeft = Alignment(-1f, 0f)

    @JvmField
    val center = Alignment(0f, 0f)

    @JvmField
    val centerRight = Alignment(1f, 0f)

    @JvmField
    val bottomLeft = Alignment(-1f, 1f)

    @JvmField
    val bottomCenter = Alignment(0f, 1f)

    @JvmField
    val bottomRight = Alignment(1f, 1f)
  }

  override val start: Float = 0f

  override fun unaryMinus() = Alignment(-x, -y)

  override fun plus(other: AlignmentFactor) = Alignment(x + other.x, y + other.y)

  override fun times(other: Float) = Alignment(x * other, y * other)

  override fun div(other: Float) = Alignment(x / other, y / other)

  override fun rem(other: Float) = Alignment(x % other, y % other)

  override fun resolve() = this
}

class AlignmentDirectional(override val start: Float, override val y: Float) : AlignmentFactor() {
  companion object {
    @JvmStatic
    fun lerp(
      a: AlignmentDirectional?,
      b: AlignmentDirectional?,
      t: Float
    ): AlignmentDirectional? {
      if (a == b) return a

      if (a == null) return AlignmentDirectional(0f.lerp(b!!.start, t), 0f.lerp(b.y, t))
      if (b == null) return AlignmentDirectional(0f.lerp(a.start, t), 0f.lerp(a.y, t))

      return AlignmentDirectional(a.start.lerp(b.start, t), a.y.lerp(b.y, t))
    }

    @JvmField
    val topStart = AlignmentDirectional(-1f, -1f)

    @JvmField
    val topCenter = AlignmentDirectional(0f, -1f)

    @JvmField
    val topEnd = AlignmentDirectional(1f, -1f)

    @JvmField
    val centerStart = AlignmentDirectional(-1f, 0f)

    @JvmField
    val center = AlignmentDirectional(0f, 0f)

    @JvmField
    val centerEnd = AlignmentDirectional(1f, 0f)

    @JvmField
    val bottomStart = AlignmentDirectional(-1f, 1f)

    @JvmField
    val bottomCenter = AlignmentDirectional(0f, 1f)

    @JvmField
    val bottomEnd = AlignmentDirectional(1f, 1f)
  }

  override val x: Float = 0f

  override fun unaryMinus() = AlignmentDirectional(-start, y)

  override fun plus(other: AlignmentFactor) = AlignmentDirectional(start + other.start, y + other.y)

  override fun times(other: Float) = AlignmentDirectional(start * other, y * other)

  override fun div(other: Float) = AlignmentDirectional(start / other, y / other)

  override fun rem(other: Float) = AlignmentDirectional(start % other, y % other)

  override fun resolve() = Alignment(start, y)
}
