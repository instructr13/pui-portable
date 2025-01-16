package dev.wycey.mido.pui.util.processing

import dev.wycey.mido.pui.layout.Point
import dev.wycey.mido.pui.layout.Size
import dev.wycey.mido.pui.renderer.BorderRadius
import processing.core.PApplet
import processing.core.PGraphics

public class AppletDrawer(
  public val applet: PApplet
) {
  private inner class AppletWith {
    private val endTasks = mutableListOf<() -> Unit>()

    fun <T> with(
      getOld: () -> T,
      set: (T) -> Unit,
      new: T
    ) {
      val old = getOld()

      set(new)

      endTasks.add { set(old) }
    }

    fun end() {
      endTasks.forEach { it() }
    }
  }

  public fun with(
    fill: Int? = null,
    stroke: Int? = null,
    tint: Int? = null,
    strokeWeight: Float? = null,
    strokeCap: StrokeCaps? = null,
    strokeJoin: StrokeJoins? = null,
    imageMode: RenderModeWithoutRadius? = null,
    rectMode: RenderMode? = null,
    ellipseMode: RenderMode? = null,
    shapeMode: RenderModeWithoutRadius? = null,
    colorMode: ColorMode? = null,
    horizontalTextAlign: TextAlign? = null,
    verticalTextAlign: VerticalTextAlign = VerticalTextAlign.Top,
    textSize: Float? = null,
    textLeading: Float? = null,
    block: () -> Unit
  ) {
    val context = AppletWith()

    if (fill != null) {
      context.with({ applet.g.fillColor }, { applet.fill(it) }, fill)
    }

    if (stroke != null) {
      context.with({ applet.g.strokeColor }, { applet.stroke(it) }, stroke)
    }

    if (tint != null) {
      context.with({ applet.g.tintColor }, { applet.tint(it) }, tint)
    }

    if (strokeWeight != null) {
      context.with({ applet.g.strokeWeight }, { applet.strokeWeight(it) }, strokeWeight)
    }

    if (strokeCap != null) {
      context.with({ applet.g.strokeCap }, { applet.strokeCap(it) }, strokeCap.get())
    }

    if (strokeJoin != null) {
      context.with({ applet.g.strokeJoin }, { applet.strokeJoin(it) }, strokeJoin.get())
    }

    if (imageMode != null) {
      context.with({ applet.g.imageMode }, { applet.imageMode(it) }, imageMode.get())
    }

    if (rectMode != null) {
      context.with({ applet.g.rectMode }, { applet.rectMode(it) }, rectMode.get())
    }

    if (ellipseMode != null) {
      context.with({ applet.g.ellipseMode }, { applet.ellipseMode(it) }, ellipseMode.get())
    }

    if (shapeMode != null) {
      context.with({ applet.g.shapeMode }, { applet.shapeMode(it) }, shapeMode.get())
    }

    if (colorMode != null) {
      context.with({ applet.g.colorMode }, { applet.colorMode(it) }, colorMode.get())
    }

    if (horizontalTextAlign != null) {
      context.with({ applet.g.textAlign }, { applet.textAlign(it, applet.g.textAlignY) }, horizontalTextAlign.get())
    }

    context.with({ applet.g.textAlignY }, { applet.textAlign(applet.g.textAlign, it) }, verticalTextAlign.get())

    if (textSize != null) {
      context.with({ applet.g.textSize }, { applet.textSize(it) }, textSize)
    }

    if (textLeading != null) {
      context.with({ applet.g.textLeading }, { applet.textLeading(it) }, textLeading)
    }

    block()

    context.end()
  }

  public fun rect(
    offset: Point,
    size: Size
  ) {
    applet.rect(offset.x, offset.y, size.width, size.height)
  }

  public fun rect(size: Size) {
    applet.rect(0f, 0f, size.width, size.height)
  }

  public fun rect(
    size: Size,
    borderRadius: BorderRadius
  ) {
    applet.rect(
      0f,
      0f,
      size.width,
      size.height,
      borderRadius.topLeft,
      borderRadius.topRight,
      borderRadius.bottomRight,
      borderRadius.bottomLeft
    )
  }

  public fun ellipse(
    offset: Point,
    size: Size
  ) {
    applet.ellipse(offset.x, offset.y, size.width, size.height)
  }

  public fun ellipse(size: Size) {
    applet.ellipse(0f, 0f, size.width, size.height)
  }

  public fun line(
    start: Point,
    end: Point
  ) {
    applet.line(start.x, start.y, end.x, end.y)
  }

  public fun text(
    content: String,
    offset: Point
  ) {
    applet.text(content, offset.x, offset.y)
  }

  public fun text(
    content: String,
    textAlign: TextAlign,
    maxWidth: Float
  ): Unit =
    text(
      content,
      Point(
        when (textAlign) {
          TextAlign.Left -> 0f
          TextAlign.Center -> maxWidth / 2
          TextAlign.Right -> maxWidth
        },
        0f
      )
    )

  public fun image(
    image: PGraphics,
    offset: Point = Point.ZERO
  ) {
    applet.image(image, offset.x, offset.y)
  }
}
