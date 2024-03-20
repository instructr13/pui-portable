package dev.wycey.mido.pui.util

import com.github.ajalt.colormath.model.RGBInt

class Colors {
  companion object {
    @JvmStatic
    fun fromRGBtoHSLuv(
      r: Int,
      g: Int,
      b: Int
    ) = RGBInt(r, g, b, 255).toHSLuv()
  }
}
