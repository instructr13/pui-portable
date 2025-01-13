package dev.wycey.mido.pui.runtime

import dev.wycey.mido.pui.bridges.PUIBridge
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.splash.SplashScreen
import dev.wycey.mido.pui.util.processing.AppletDrawer
import processing.core.PApplet

class PUIRuntime(private val applet: PApplet) {
  @JvmOverloads
  fun mount(
    app: Component,
    splashScreen: SplashScreen? = null
  ) {
    (splashScreen ?: SplashScreen(Size(applet.width.toFloat(), applet.height.toFloat()))).draw(AppletDrawer(applet))

    PUIBridge.init(applet).apply {
      attachRootComponent(wrapWithProcessingView(app))
      registerDrawEventLoop()
    }
  }
}
