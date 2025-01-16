package dev.wycey.mido.pui.splash

import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.util.processing.AppletDrawer

public class SplashScreen(
  private val windowSize: Size
) {
  public fun draw(d: AppletDrawer) {
    d.applet.background(255)

    val center = windowSize.toPoint() / Point(2f, 2f)

    d.with(fill = 0xff000000.toInt()) {
      d.rect(center - Point(20f, 0f), Size(8f, 8f))
      d.rect(center + Point(0f, 0f), Size(8f, 8f))
      d.rect(center + Point(20f, 0f), Size(8f, 8f))
    }
    /*
    d.with(textSize = 32f, horizontalTextAlign = TextAlign.Left, verticalTextAlign = VerticalTextAlign.Center) {
      d.text("Loading...", windowSize.toPoint() / Point(2, 2))
    }
     */
  }
}
