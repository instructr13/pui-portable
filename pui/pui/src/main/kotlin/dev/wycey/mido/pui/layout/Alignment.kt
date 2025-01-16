package dev.wycey.mido.pui.layout

import dev.wycey.mido.pui.layout.constraints.lerp

public abstract class AlignmentFactor {
  public companion object {
    @JvmStatic
    public fun lerp(
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

  private data class MixedAlignment(
    override val x: Float,
    override val start: Float,
    override val y: Float
  ) : AlignmentFactor() {
    override fun unaryMinus() = MixedAlignment(-x, -start, -y)

    override fun plus(other: AlignmentFactor) = MixedAlignment(x + other.x, start + other.start, y + other.y)

    override fun times(other: Float) = MixedAlignment(x * other, start * other, y * other)

    override fun div(other: Float) = MixedAlignment(x / other, start / other, y / other)

    override fun rem(other: Float) = MixedAlignment(x % other, start % other, y % other)

    override fun resolve() = Alignment(x + start, y)
  }

  public abstract val x: Float
  public abstract val start: Float
  public abstract val y: Float

  public abstract operator fun unaryMinus(): AlignmentFactor

  public abstract operator fun plus(other: AlignmentFactor): AlignmentFactor

  public abstract operator fun times(other: Float): AlignmentFactor

  public abstract operator fun div(other: Float): AlignmentFactor

  public abstract operator fun rem(other: Float): AlignmentFactor

  public fun alongOffset(offset: Point): Point {
    val centerX = offset.x / 2
    val centerY = offset.y / 2

    return Point(
      centerX + x * centerX + start * offset.x,
      centerY + y * centerY
    )
  }

  override fun equals(other: Any?): Boolean =
    other is AlignmentFactor && x == other.x && start == other.start && y == other.y

  override fun hashCode(): Int {
    var result = x.hashCode()
    result = 31 * result + start.hashCode()
    result = 31 * result + y.hashCode()
    return result
  }

  internal abstract fun resolve(): Alignment
}

public class Alignment(
  override val x: Float,
  override val y: Float
) : AlignmentFactor() {
  public companion object {
    @JvmStatic
    public fun lerp(
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
    public val topLeft: Alignment = Alignment(-1f, -1f)

    @JvmField
    public val topCenter: Alignment = Alignment(0f, -1f)

    @JvmField
    public val topRight: Alignment = Alignment(1f, -1f)

    @JvmField
    public val centerLeft: Alignment = Alignment(-1f, 0f)

    @JvmField
    public val center: Alignment = Alignment(0f, 0f)

    @JvmField
    public val centerRight: Alignment = Alignment(1f, 0f)

    @JvmField
    public val bottomLeft: Alignment = Alignment(-1f, 1f)

    @JvmField
    public val bottomCenter: Alignment = Alignment(0f, 1f)

    @JvmField
    public val bottomRight: Alignment = Alignment(1f, 1f)
  }

  override val start: Float = 0f

  override fun unaryMinus(): Alignment = Alignment(-x, -y)

  override fun plus(other: AlignmentFactor): Alignment = Alignment(x + other.x, y + other.y)

  override fun times(other: Float): Alignment = Alignment(x * other, y * other)

  override fun div(other: Float): Alignment = Alignment(x / other, y / other)

  override fun rem(other: Float): Alignment = Alignment(x % other, y % other)

  override fun resolve(): Alignment = this
}

public class AlignmentDirectional(
  override val start: Float,
  override val y: Float
) : AlignmentFactor() {
  public companion object {
    @JvmStatic
    public fun lerp(
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
    public val topStart: AlignmentDirectional = AlignmentDirectional(-1f, -1f)

    @JvmField
    public val topCenter: AlignmentDirectional = AlignmentDirectional(0f, -1f)

    @JvmField
    public val topEnd: AlignmentDirectional = AlignmentDirectional(1f, -1f)

    @JvmField
    public val centerStart: AlignmentDirectional = AlignmentDirectional(-1f, 0f)

    @JvmField
    public val center: AlignmentDirectional = AlignmentDirectional(0f, 0f)

    @JvmField
    public val centerEnd: AlignmentDirectional = AlignmentDirectional(1f, 0f)

    @JvmField
    public val bottomStart: AlignmentDirectional = AlignmentDirectional(-1f, 1f)

    @JvmField
    public val bottomCenter: AlignmentDirectional = AlignmentDirectional(0f, 1f)

    @JvmField
    public val bottomEnd: AlignmentDirectional = AlignmentDirectional(1f, 1f)
  }

  override val x: Float = 0f

  override fun unaryMinus(): AlignmentDirectional = AlignmentDirectional(-start, y)

  override fun plus(other: AlignmentFactor): AlignmentDirectional =
    AlignmentDirectional(start + other.start, y + other.y)

  override fun times(other: Float): AlignmentDirectional = AlignmentDirectional(start * other, y * other)

  override fun div(other: Float): AlignmentDirectional = AlignmentDirectional(start / other, y / other)

  override fun rem(other: Float): AlignmentDirectional = AlignmentDirectional(start % other, y % other)

  override fun resolve() = Alignment(start, y)
}
