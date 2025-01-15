package dev.wycey.mido.leinwand.util

import kotlin.math.round

internal fun Float.round(decimals: Int): Float {
  var multiplier = 1.0

  repeat(decimals) { multiplier *= 10 }

  return (round(this * multiplier) / multiplier).toFloat()
}
