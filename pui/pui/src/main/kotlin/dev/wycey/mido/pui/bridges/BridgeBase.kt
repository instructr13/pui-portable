package dev.wycey.mido.pui.bridges

import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.lazyOnce
import processing.core.PApplet

internal interface BridgeBaseContract {
  fun initInstance()
}

public abstract class BridgeBase : BridgeBaseContract {
  public companion object {
    @JvmStatic
    internal fun <T : BridgeBase> checkInstance(instance: T?): T =
      instance ?: throw NullPointerException("Bridge instance is null")

    @JvmStatic
    public var applet: PApplet by lazyOnce()

    @JvmStatic
    internal val rootScope: Scope by lazy {
      Scope(applet)
    }
  }

  override fun initInstance() {}
}

public abstract class BridgeBaseWithAutoInit : BridgeBase() {
  init {
    this.initInstance()
  }
}
