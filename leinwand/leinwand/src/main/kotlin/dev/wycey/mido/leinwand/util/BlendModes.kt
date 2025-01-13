package dev.wycey.mido.leinwand.util

import processing.core.PApplet

object BlendModes {
  val blendNameToBlendMode =
    mapOf(
      "通常" to PApplet.NORMAL,
      "加算" to PApplet.ADD,
      "減算" to PApplet.SUBTRACT,
      "比較 (明)" to PApplet.LIGHTEST,
      "比較 (暗)" to PApplet.DARKEST,
      "色差" to PApplet.DIFFERENCE,
      "除外" to PApplet.EXCLUSION,
      "乗算" to PApplet.MULTIPLY,
      "スクリーン" to PApplet.SCREEN,
      "オーバーレイ" to PApplet.OVERLAY,
      "ハードライト" to PApplet.HARD_LIGHT,
      "ソフトライト" to PApplet.SOFT_LIGHT,
      "覆い焼き" to PApplet.DODGE,
      "焼き込み" to PApplet.BURN
    )

  val blendModeToBlendName = blendNameToBlendMode.entries.associate { (k, v) -> v to k }
}
