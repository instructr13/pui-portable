package dev.wycey.mido.pui.util

import kotlin.math.round

fun Float.round(decimals: Int): Float {
  var multiplier = 1.0

  repeat(decimals) { multiplier *= 10 }

  return (round(this * multiplier) / multiplier).toFloat()
}
