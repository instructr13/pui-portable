package dev.wycey.mido.pui.layout

public class EdgeInsets(
  public val top: Float,
  public val right: Float,
  public val bottom: Float,
  public val left: Float
) {
  public companion object {
    @JvmField
    public val ZERO: EdgeInsets = EdgeInsets(0f, 0f, 0f, 0f)

    @JvmStatic
    @JvmOverloads
    public fun symmetric(
      vertical: Float = 0f,
      horizontal: Float = 0f
    ): EdgeInsets = EdgeInsets(vertical, horizontal, vertical, horizontal)

    @JvmStatic
    public fun all(value: Float): EdgeInsets = EdgeInsets(value, value, value, value)

    @JvmStatic
    @JvmOverloads
    public fun only(
      top: Float = 0f,
      right: Float = 0f,
      bottom: Float = 0f,
      left: Float = 0f
    ): EdgeInsets = EdgeInsets(top, right, bottom, left)
  }

  public fun applyDeltaXY(anchor: Point): Point = anchor + Point(left, top)

  public fun toSize(): Size = Size(left + right, top + bottom)

  public fun getConstraint(initialSize: Size): Size = initialSize - toSize()

  public fun padSize(initialSize: Size): Size = initialSize + toSize()
}
