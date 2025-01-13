package dev.wycey.mido.pui.layout.constraints

import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.layout.Size

internal fun Float.lerp(
  other: Float,
  t: Float
) = this * (1 - t) + other * t

data class BoxConstraints(
  var minWidth: Float = 0f,
  var maxWidth: Float = Float.POSITIVE_INFINITY,
  var minHeight: Float = 0f,
  var maxHeight: Float = Float.POSITIVE_INFINITY
) : Constraints() {
  companion object {
    @JvmStatic
    fun tight(size: Size) = BoxConstraints(size.width, size.width, size.height, size.height)

    @JvmStatic
    fun tightFor(
      width: Float? = null,
      height: Float? = null
    ) = BoxConstraints(
      width ?: 0f,
      width ?: Float.POSITIVE_INFINITY,
      height ?: 0f,
      height ?: Float.POSITIVE_INFINITY
    )

    @JvmStatic
    fun lerp(
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

  val hasTightWidth get() = minWidth >= maxWidth
  val hasTightHeight get() = minHeight >= maxHeight
  val hasBoundedWidth get() = maxWidth < Float.POSITIVE_INFINITY
  val hasBoundedHeight get() = maxHeight < Float.POSITIVE_INFINITY
  val smallest get() = Size(constrainWidth(0f), constrainHeight(0f))
  val biggest get() = Size(constrainWidth(Float.POSITIVE_INFINITY), constrainHeight(Float.POSITIVE_INFINITY))
  val tightest get() = Size(constrainWidth(), constrainHeight())
  val loosest get() = Size(constrainWidth(Float.POSITIVE_INFINITY), constrainHeight(Float.POSITIVE_INFINITY))
  val isNormalized get() = minWidth <= maxWidth && minHeight <= maxHeight
  val maxSize get() = Size(maxWidth, maxHeight)

  override val isTight: Boolean
    get() = hasTightWidth && hasTightHeight

  fun isFinite() = minWidth.isFinite() && maxWidth.isFinite() && minHeight.isFinite() && maxHeight.isFinite()

  fun enforce(constraints: BoxConstraints) =
    BoxConstraints(
      minWidth.coerceIn(constraints.minWidth..constraints.maxWidth),
      maxWidth.coerceIn(constraints.minWidth..constraints.maxWidth),
      minHeight.coerceIn(constraints.minHeight..constraints.maxHeight),
      maxHeight.coerceIn(constraints.minHeight..constraints.maxHeight)
    )

  fun constrainWidth(width: Float = Float.POSITIVE_INFINITY) = width.coerceIn(minWidth..maxWidth)

  fun constrainHeight(height: Float = Float.POSITIVE_INFINITY) = height.coerceIn(minHeight..maxHeight)

  fun constrain(size: Size) = Size(constrainWidth(size.width), constrainHeight(size.height))

  fun constrainDimensions(
    width: Float,
    height: Float
  ) = Size(constrainWidth(width), constrainHeight(height))

  fun deflate(edges: EdgeInsets): BoxConstraints {
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

  fun tighten(
    width: Float? = null,
    height: Float? = null
  ) = BoxConstraints(
    width?.coerceIn(minWidth..maxWidth) ?: minWidth,
    width?.coerceIn(minWidth..maxWidth) ?: maxWidth,
    height?.coerceIn(minHeight..maxHeight) ?: minHeight,
    height?.coerceIn(minHeight..maxHeight) ?: maxHeight
  )

  fun loosen() =
    BoxConstraints(
      maxWidth = maxWidth,
      maxHeight = maxHeight
    )

  operator fun times(factor: Float) =
    BoxConstraints(
      minWidth * factor,
      maxWidth * factor,
      minHeight * factor,
      maxHeight * factor
    )

  operator fun div(factor: Float) =
    BoxConstraints(
      minWidth / factor,
      maxWidth / factor,
      minHeight / factor,
      maxHeight / factor
    )

  operator fun rem(factor: Float) =
    BoxConstraints(
      minWidth % factor,
      maxWidth % factor,
      minHeight % factor,
      maxHeight % factor
    )
}
