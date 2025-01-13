package dev.wycey.mido.pui.layout

class EdgeInsets(val top: Float, val right: Float, val bottom: Float, val left: Float) {
  companion object {
    @JvmField
    val ZERO = EdgeInsets(0f, 0f, 0f, 0f)

    @JvmStatic
    @JvmOverloads
    fun symmetric(
      vertical: Float = 0f,
      horizontal: Float = 0f
    ) = EdgeInsets(vertical, horizontal, vertical, horizontal)

    @JvmStatic
    fun all(value: Float) = EdgeInsets(value, value, value, value)

    @JvmStatic
    @JvmOverloads
    fun only(
      top: Float = 0f,
      right: Float = 0f,
      bottom: Float = 0f,
      left: Float = 0f
    ) = EdgeInsets(top, right, bottom, left)
  }

  fun applyDeltaXY(anchor: Point) = anchor + Point(left, top)

  fun toSize() = Size(left + right, top + bottom)

  fun getConstraint(initialSize: Size) = initialSize - toSize()

  fun padSize(initialSize: Size) = initialSize + toSize()
}
