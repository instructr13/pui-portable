package dev.wycey.mido.pui.renderer.text

import dev.wycey.mido.pui.bridges.BridgeBase.Companion.applet
import dev.wycey.mido.pui.components.text.TextStyle
import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.layout.constraints.BoxConstraints
import dev.wycey.mido.pui.renderer.box.BoxRenderer
import dev.wycey.mido.pui.util.Scope
import dev.wycey.mido.pui.util.processing.AppletDrawer
import dev.wycey.mido.pui.util.processing.TextAlign
import dev.wycey.mido.pui.util.processing.VerticalTextAlign
import processing.core.PApplet

public class TextRenderer(initialContent: String, initialTextStyle: TextStyle) : BoxRenderer() {
  private var textAscent: Float = 0f
  private var textDescent: Float = 0f

  public var content: String = initialContent
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayout()
    }

  public var textStyle: TextStyle = initialTextStyle
    set(value) {
      if (field == value) return

      field = value

      markNeedsLayout()
    }

  private val contentLines get() = content.split("\n")
  private val lastLineY get() = textAscent + textDescent
  private val widthPerLines get() = contentLines.map { applet.textWidth(it) }

  private fun computeSize(
    applet: PApplet,
    constraints: BoxConstraints
  ): Size {
    applet.pushStyle()

    applet.textSize(textStyle.fontSize)

    textStyle.textAlign.apply(applet)

    textAscent = applet.textAscent()
    textDescent = applet.textDescent()

    val y = lastLineY + (contentLines.size - 1) * applet.g.textLeading

    applet.popStyle()

    return constraints.constrain(Size(widthPerLines.maxOrNull() ?: 0f, y))
  }

  override fun computeDryLayout(constraints: BoxConstraints): Size = computeSize(applet, constraints)

  override fun performLayout() {
    size = computeSize(applet, constraints as BoxConstraints)
  }

  override fun paint(
    d: AppletDrawer,
    currentScope: Scope
  ) {
    d.with(
      fill = textStyle.color,
      horizontalTextAlign = textStyle.textAlign,
      verticalTextAlign = VerticalTextAlign.Top,
      textSize = textStyle.fontSize
    ) {
      var y = 0f

      widthPerLines.forEachIndexed { i, width ->
        d.with(fill = textStyle.backgroundColor) {
          applet.noStroke()

          val dy = if (i == contentLines.size - 1) lastLineY else applet.g.textLeading

          val x =
            when (textStyle.textAlign) {
              TextAlign.Left -> 0f
              TextAlign.Center -> (size.width - width) / 2
              TextAlign.Right -> size.width - width
            }

          d.rect(
            Point(
              x,
              y
            ),
            Size(
              width,
              dy + if (i < contentLines.size) 1f else 0f
            )
          )

          y += dy
        }
      }

      d.text(content, textStyle.textAlign, size.width)
    }
  }
}
