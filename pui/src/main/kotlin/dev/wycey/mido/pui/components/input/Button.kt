package dev.wycey.mido.pui.components.input

import dev.wycey.mido.pui.bridges.BridgeBase.Companion.applet
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponentWithChild
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventArgs
import dev.wycey.mido.pui.events.mouse.gestures.GestureEventType
import dev.wycey.mido.pui.util.processing.CursorType

class Button
  @JvmOverloads
  constructor(
    private val width: Float? = null,
    private val height: Float? = null,
    private val onClick: ((e: GestureEventArgs, type: GestureEventType.Click) -> Unit)? = null,
    private val onPress: ((e: GestureEventArgs, type: GestureEventType.Press) -> Unit)? = null,
    private val onRelease: ((e: GestureEventArgs, type: GestureEventType.Release) -> Unit)? = null,
    key: String? = null,
    private val styleBuilder: (() -> ButtonStyle)? = null,
    childBuilder: (() -> Component)? = null
  ) : StatefulComponentWithChild(key, childBuilder) {
    @JvmOverloads
    constructor(
      child: Component,
      style: ButtonStyle = ButtonStyle(),
      width: Float? = null,
      height: Float? = null,
      onClick: ((e: GestureEventArgs, type: GestureEventType.Click) -> Unit)? = null,
      onPress: ((e: GestureEventArgs, type: GestureEventType.Press) -> Unit)? = null,
      onRelease: ((e: GestureEventArgs, type: GestureEventType.Release) -> Unit)? = null,
      key: String? = null
    ) : this(width, height, onClick, onPress, onRelease, key, { style }, { child })

    override fun build(context: BuildContext): Component {
      val style = styleBuilder?.invoke() ?: ButtonStyle()

      var hovering by signal(false)
      var pressing by signal(false)

      val fill =
        when {
          style.disabled -> style.disabledColor
          pressing -> style.pressedColor
          hovering -> style.hoverColor
          else -> style.normalColor
        }

      val onPress =
        createFunction {
          pressing = true
        }

      val onRelease =
        createFunction {
          pressing = false
        }

      val onHover =
        createFunction {
          if (style.disabled) return@createFunction

          hovering = true

          CursorType.Hand.apply(applet)
        }

      val onLeave =
        createFunction {
          if (style.disabled) return@createFunction

          hovering = false

          CursorType.Arrow.apply(applet)
        }

      if (width != null || height != null) {
        return GestureListener(
          VirtualBox(
            Box(
              childBuilder?.invoke(),
              fill = fill,
              borderRadius = style.borderRadius,
              stroke = 0,
              strokeWeight = 0f
            ),
            width = width,
            height = height
          ),
          onPress = { e, type ->
            if (!style.disabled && type.button == MouseButtons.LEFT) {
              onPress()

              this.onPress?.invoke(e, type)
            }
          },
          onRelease = { e, type ->
            if (!style.disabled && type.button == MouseButtons.LEFT) {
              onRelease()

              this.onRelease?.invoke(e, type)
            }
          },
          onHover = { _, _ -> onHover() },
          onLeave = { _, _ -> onLeave() },
          onClick = { e, type ->
            if (type.button == MouseButtons.LEFT) {
              onClick?.invoke(e, type)
            }
          }
        )
      }

      return GestureListener(
        VirtualBox(
          Box(
            Padding(
              style.childPadding,
              childBuilder?.invoke()
            ),
            fill = fill,
            borderRadius = style.borderRadius,
            stroke = style.borderColor,
            strokeWeight = 1f
          )
        ),
        onPress = { e, type ->
          if (!style.disabled && type.button == MouseButtons.LEFT) {
            onPress()

            this.onPress?.invoke(e, type)
          }
        },
        onRelease = { e, type ->
          if (!style.disabled && type.button == MouseButtons.LEFT) {
            onRelease()

            this.onRelease?.invoke(e, type)
          }
        },
        onHover = { _, _ -> onHover() },
        onLeave = { _, _ -> onLeave() },
        onClick = { e, type ->
          if (type.button == MouseButtons.LEFT) {
            onClick?.invoke(e, type)
          }
        }
      )
    }
  }
