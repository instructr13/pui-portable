package dev.wycey.mido.pui.components.input

import dev.wycey.mido.pui.bridges.BridgeBase.Companion.applet
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.gestures.GestureListener
import dev.wycey.mido.pui.components.layout.Center
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.layout.VirtualBox
import dev.wycey.mido.pui.components.layout.ZStack
import dev.wycey.mido.pui.components.output.ProgressBar
import dev.wycey.mido.pui.components.processing.Ellipse
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.state.signals.untracked
import dev.wycey.mido.pui.util.processing.CursorType
import kotlin.time.Duration.Companion.milliseconds

public class Slider
  @JvmOverloads
  constructor(
    private val sliderValueBuilder: () -> Float,
    private val min: Float = 0f,
    private val max: Float = 100f,
    private val step: Float = 1f,
    private val barSize: Float = 200f,
    private val barThickness: Float = 4f,
    private val barHoverThicknessFactor: Float = 1.5f,
    private val additionalPadding: EdgeInsets = EdgeInsets.all(10f),
    private val barColor: Int = 0xFFDFDFDF.toInt(),
    private val thumbColor: Int = 0xFFFFFFFF.toInt(),
    private val hoverThumbColor: Int = 0xFFE0E0E0.toInt(),
    private val pressedThumbColor: Int = 0xFFC0C0C0.toInt(),
    private val trackColor: Int = 0xFF5886E0.toInt(),
    private val thumbSize: Float = 20f,
    key: String? = null,
    private val onChange: ((Float) -> Unit)? = null
  ) : StatefulComponent(key) {
    @JvmOverloads
    public constructor(
      sliderValue: Float,
      min: Float = 0f,
      max: Float = 100f,
      step: Float = 1f,
      barSize: Float = 200f,
      barThickness: Float = 4f,
      barHoverThicknessFactor: Float = 1.5f,
      additionalPadding: EdgeInsets = EdgeInsets.all(10f),
      barColor: Int = 0xFFDFDFDF.toInt(),
      thumbColor: Int = 0xFFFFFFFF.toInt(),
      hoverThumbColor: Int = 0xFFE0E0E0.toInt(),
      pressedThumbColor: Int = 0xFFC0C0C0.toInt(),
      trackColor: Int = 0xFF5886E0.toInt(),
      thumbSize: Float = 20f,
      key: String? = null,
      onChange: ((Float) -> Unit)? = null
    ) : this(
      { sliderValue },
      min,
      max,
      step,
      barSize,
      barThickness,
      barHoverThicknessFactor,
      additionalPadding,
      barColor,
      thumbColor,
      hoverThumbColor,
      pressedThumbColor,
      trackColor,
      thumbSize,
      key,
      onChange
    )

    private fun setValue(value: Float) {
      onChange?.let { it(value) }
    }

    private fun roundValue(value: Float): Float {
      val remainder = value % step

      return if (max - (max % step) < value) {
        value
      } else if (remainder < step / 2) {
        value - remainder
      } else {
        value + step - remainder
      }
    }

    override fun build(context: BuildContext): Component {
      val sliderValue = sliderValueBuilder()
      var sliderValueBeforeDrag: Float? by signal(null)

      var hovering by signal(false)
      var hoveringLonger by signal(false)
      var pressing by signal(false)

      val thumbColor =
        untracked {
          if (pressing) {
            pressedThumbColor
          } else if (hovering) {
            hoverThumbColor
          } else {
            thumbColor
          }
        }

      val onPress =
        createFunction {
          pressing = true
          hoveringLonger = true
        }

      val onRelease =
        createFunction {
          pressing = false
        }

      val onHover =
        createFunction {
          hovering = true

          CursorType.Hand.apply(applet)
        }

      val onLeave =
        createFunction {
          hovering = false
          hoveringLonger = false

          CursorType.Arrow.apply(applet)
        }

      val thumb =
        GestureListener(
          VirtualBox(
            Ellipse(
              fill = thumbColor,
              stroke = 0,
              strokeWeight = 0f
            ),
            width = thumbSize,
            height = thumbSize
          ),
          onPress = { _, type ->
            if (type.button == MouseButtons.LEFT) {
              onPress()
            }
          },
          onRelease = { _, type ->
            if (type.button == MouseButtons.LEFT) {
              onRelease()
            }
          },
          onHover = { _, _ -> onHover() },
          onLeave = { _, _ -> onLeave() },
          onDrag = onDrag@{ e, type ->
            if (type.button != MouseButtons.LEFT) return@onDrag

            if (sliderValueBeforeDrag == null) {
              sliderValueBeforeDrag = untracked { sliderValue }
            }

            val startX = type.startingPoint.x
            val currentX = e.x

            val newValue =
              ((currentX - startX) / barSize * (max - min) + untracked { sliderValueBeforeDrag!! }).coerceIn(
                min,
                max
              )

            val newValueRounded = roundValue(newValue).coerceIn(min, max)

            setValue(newValueRounded)
          },
          onDrop = { _, type ->
            if (type.button == MouseButtons.LEFT) {
              sliderValueBeforeDrag = null
            }
          }
        )

      val trackSize = sliderValue / (max - min) * barSize

      return VirtualBox(
        ZStack(
          listOf(
            Center(
              GestureListener(
                ProgressBar(
                  value = sliderValue,
                  min = min,
                  max = max,
                  barSize = barSize,
                  barThickness = if (hoveringLonger) barThickness * barHoverThicknessFactor else barThickness,
                  barColor = barColor,
                  trackColor = trackColor
                ),
                onPress = { e, _ ->
                  val newValue = ((e.delta.first / barSize) * (max - min)).coerceIn(min, max)
                  val newValueRounded = roundValue(newValue).coerceIn(min, max)

                  setValue(newValueRounded)

                  pressing = true
                },
                onRelease = { _, _ ->
                  pressing = false
                },
                onHover = { _, type ->
                  if (type.duration > 200.milliseconds) {
                    hoveringLonger = true

                    CursorType.Hand.apply(applet)
                  }
                },
                onLeave = { _, _ ->
                  hoveringLonger = false

                  CursorType.Arrow.apply(applet)
                },
                onDrag = onDrag@{ e, type ->
                  if (type.button != MouseButtons.LEFT) return@onDrag

                  val newValue = ((e.delta.first / barSize) * (max - min)).coerceIn(min, max)
                  val newValueRounded = roundValue(newValue).coerceIn(min, max)

                  setValue(newValueRounded)
                },
                onDrop = onDrop@{ _, type ->
                  if (type.button != MouseButtons.LEFT) return@onDrop

                  pressing = false
                }
              )
            ),
            Padding(
              EdgeInsets.only(
                left = trackSize,
                top = additionalPadding.top
              ),
              thumb
            )
          )
        ),
        width = barSize + additionalPadding.left + additionalPadding.right,
        height = thumbSize + additionalPadding.top + additionalPadding.bottom
      )
    }
  }
