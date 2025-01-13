package dev.wycey.mido.pui.bridges

import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.lazyOnce
import processing.core.PApplet

interface BridgeBaseContract {
  fun initInstance()
}

abstract class BridgeBase : BridgeBaseContract {
  companion object {
    @JvmStatic
    fun <T : BridgeBase> checkInstance(instance: T?): T =
      instance ?: throw NullPointerException("Bridge instance is null")

    @JvmStatic
    var applet: PApplet by lazyOnce()

    @JvmStatic
    val rootScope: Scope by lazy {
      Scope(applet)
    }
  }

  override fun initInstance() {}
}

abstract class BridgeBaseWithAutoInit : BridgeBase() {
  init {
    this.initInstance()
  }
}
