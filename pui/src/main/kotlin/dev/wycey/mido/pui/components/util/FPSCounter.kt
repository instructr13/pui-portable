package dev.wycey.mido.pui.components.util

import dev.wycey.mido.pui.bridges.BridgeBase.Companion.applet
import dev.wycey.mido.pui.bridges.FrameEventBridge
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.util.round

class FPSCounter
  @JvmOverloads
  constructor(
    private val prefix: String = "FPS: ",
    private val decimalRoundTo: Int = 1,
    key: String? = null
  ) :
  StatefulComponent(key) {
    override fun build(context: BuildContext): Component {
      var fps by signal(0f)

      FrameEventBridge.instance.onPostDraw {
        fps = applet.frameRate
      }

      return Text("$prefix${fps.round(decimalRoundTo)}")
    }
  }
