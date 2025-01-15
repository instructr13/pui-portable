package dev.wycey.mido.pui.layout.constraints

import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.layout.Size

internal fun Float.lerp(
  other: Float,
  t: Float
) = this * (1 - t) + other * t

public data class BoxConstraints(
  var minWidth: Float = 0f,
  var maxWidth: Float = Float.POSITIVE_INFINITY,
  var minHeight: Float = 0f,
  var maxHeight: Float = Float.POSITIVE_INFINITY
) : Constraints() {
  public companion object {
    @JvmStatic
    public fun tight(size: Size): BoxConstraints = BoxConstraints(size.width, size.width, size.height, size.height)

    @JvmStatic
    public fun tightFor(
      width: Float? = null,
      height: Float? = null
    ): BoxConstraints =
      BoxConstraints(
        width ?: 0f,
        width ?: Float.POSITIVE_INFINITY,
        height ?: 0f,
        height ?: Float.POSITIVE_INFINITY
      )

    @JvmStatic
    public fun lerp(
      a: BoxConstraints?,
      b: BoxConstraints?,
      t: Float
    ): BoxConstraints? {
      if (a == b) {
        return a
      }

      if (a == null) {
        return b!! * t
      }

      if (b == null) {
        return a * (1 - t)
      }

      return BoxConstraints(
        if (a.minWidth.isFinite()) a.minWidth.lerp(b.minWidth, t) else Float.POSITIVE_INFINITY,
        if (a.maxWidth.isFinite()) a.maxWidth.lerp(b.maxWidth, t) else Float.POSITIVE_INFINITY,
        if (a.minHeight.isFinite()) a.minHeight.lerp(b.minHeight, t) else Float.POSITIVE_INFINITY,
        if (a.maxHeight.isFinite()) a.maxHeight.lerp(b.maxHeight, t) else Float.POSITIVE_INFINITY
      )
    }
  }

  val hasTightWidth: Boolean get() = minWidth >= maxWidth
  val hasTightHeight: Boolean get() = minHeight >= maxHeight
  val hasBoundedWidth: Boolean get() = maxWidth < Float.POSITIVE_INFINITY
  val hasBoundedHeight: Boolean get() = maxHeight < Float.POSITIVE_INFINITY
  val smallest: Size get() = Size(constrainWidth(0f), constrainHeight(0f))
  val biggest: Size get() = Size(constrainWidth(Float.POSITIVE_INFINITY), constrainHeight(Float.POSITIVE_INFINITY))
  val tightest: Size get() = Size(constrainWidth(), constrainHeight())
  val loosest: Size get() = Size(constrainWidth(Float.POSITIVE_INFINITY), constrainHeight(Float.POSITIVE_INFINITY))
  val isNormalized: Boolean get() = minWidth <= maxWidth && minHeight <= maxHeight
  val maxSize: Size get() = Size(maxWidth, maxHeight)

  override val isTight: Boolean
    get() = hasTightWidth && hasTightHeight

  public fun isFinite(): Boolean =
    minWidth.isFinite() && maxWidth.isFinite() && minHeight.isFinite() && maxHeight.isFinite()

  public fun enforce(constraints: BoxConstraints): BoxConstraints =
    BoxConstraints(
      minWidth.coerceIn(constraints.minWidth..constraints.maxWidth),
      maxWidth.coerceIn(constraints.minWidth..constraints.maxWidth),
      minHeight.coerceIn(constraints.minHeight..constraints.maxHeight),
      maxHeight.coerceIn(constraints.minHeight..constraints.maxHeight)
    )

  public fun constrainWidth(width: Float = Float.POSITIVE_INFINITY): Float = width.coerceIn(minWidth..maxWidth)

  public fun constrainHeight(height: Float = Float.POSITIVE_INFINITY): Float = height.coerceIn(minHeight..maxHeight)

  public fun constrain(size: Size): Size = Size(constrainWidth(size.width), constrainHeight(size.height))

  public fun constrainDimensions(
    width: Float,
    height: Float
  ): Size = Size(constrainWidth(width), constrainHeight(height))

  public fun deflate(edges: EdgeInsets): BoxConstraints {
    val horizontal = edges.left + edges.right
    val vertical = edges.top + edges.bottom
    val deflatedMinWidth = maxOf(0f, minWidth - horizontal)
    val deflatedMinHeight = maxOf(0f, minHeight - vertical)

    return BoxConstraints(
      deflatedMinWidth,
      maxOf(deflatedMinWidth, maxWidth - horizontal),
      deflatedMinHeight,
      maxOf(deflatedMinHeight, maxHeight - vertical)
    )
  }

  public fun tighten(
    width: Float? = null,
    height: Float? = null
  ): BoxConstraints =
    BoxConstraints(
      width?.coerceIn(minWidth..maxWidth) ?: minWidth,
      width?.coerceIn(minWidth..maxWidth) ?: maxWidth,
      height?.coerceIn(minHeight..maxHeight) ?: minHeight,
      height?.coerceIn(minHeight..maxHeight) ?: maxHeight
    )

  public fun loosen(): BoxConstraints =
    BoxConstraints(
      maxWidth = maxWidth,
      maxHeight = maxHeight
    )

  public operator fun times(factor: Float): BoxConstraints =
    BoxConstraints(
      minWidth * factor,
      maxWidth * factor,
      minHeight * factor,
      maxHeight * factor
    )

  public operator fun div(factor: Float): BoxConstraints =
    BoxConstraints(
      minWidth / factor,
      maxWidth / factor,
      minHeight / factor,
      maxHeight / factor
    )

  public operator fun rem(factor: Float): BoxConstraints =
    BoxConstraints(
      minWidth % factor,
      maxWidth % factor,
      minHeight % factor,
      maxHeight % factor
    )
}
