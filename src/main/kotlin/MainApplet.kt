import dev.wycey.mido.pui.components.basic.StatefulComponent
import dev.wycey.mido.pui.components.input.Checkbox
import dev.wycey.mido.pui.components.input.Slider
import dev.wycey.mido.pui.components.layout.*
import dev.wycey.mido.pui.components.metalayout.Tabs
import dev.wycey.mido.pui.components.metalayout.TitleBar
import dev.wycey.mido.pui.components.processing.Box
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.components.text.TextStyle
import dev.wycey.mido.pui.components.util.FPSCounter
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.examples.SimpleChip
import dev.wycey.mido.pui.examples.colors.HSLuvColorPicker
import dev.wycey.mido.pui.layout.Alignment
import dev.wycey.mido.pui.layout.EdgeInsets
import dev.wycey.mido.pui.renderer.layout.StackCrossAxisAlignment
import dev.wycey.mido.pui.runtime.PUIRuntime
import processing.core.PApplet
import processing.core.PFont

class MainApplet : PApplet() {
  private val runtime: PUIRuntime = PUIRuntime(this)
  private lateinit var font: PFont

  override fun settings() {
    size(1300, 900)
  }

  override fun setup() {
    font = loadFont("NotoSansCJKjp-Regular-20.vlw")
    textFont(font)

    windowTitle("PUI Portable")

    runtime.mount(
      object : StatefulComponent() {
        override fun build(context: BuildContext): ZStack {
          var sliderValue by signal(50f)
          var selectedTabIndex by signal(0)

          return ZStack(
            listOf(
              Align(
                Alignment.bottomLeft,
                FPSCounter()
              ),
              Align(
                Alignment.topLeft,
                VStack(
                  listOf(
                    TitleBar(
                      "PUI Portable",
                      "by instructr13",
                      additionalArea = { _ ->
                        arrayOf(
                          VStack(
                            listOf(
                              SimpleChip(
                                "Processing",
                                "Ready for interaction"
                              ),
                              VirtualBox(height = 6f),
                              SimpleChip(
                                "Pico",
                                "Waiting for connection"
                              )
                            )
                          )
                        )
                      }
                    ),
                    Expanded(
                      Padding(
                        EdgeInsets.all(20f),
                        VStack(
                          listOf(
                            HStack(
                              listOf(
                                Expanded(
                                  VStack(
                                    listOf(
                                      Tabs(
                                        listOf(
                                          "Operation" to
                                            VStack(
                                              listOf(
                                                Text("Operation Content")
                                              )
                                            ),
                                          "Pico Information" to Text("Pico Information Content")
                                        ),
                                        selectedTabIndex,
                                        {
                                          selectedTabIndex = it
                                        }
                                      ),
                                      Padding(
                                        EdgeInsets.only(bottom = 4f),
                                        Text("Operation", TextStyle(fontSize = 30f))
                                      ),
                                      Checkbox(
                                        true,
                                        label = "Builtin LED"
                                      ),
                                      Padding(
                                        EdgeInsets.symmetric(vertical = 10f),
                                        Box(
                                          Padding(
                                            EdgeInsets.symmetric(
                                              vertical = 10f,
                                              horizontal = 6f
                                            ),
                                            VStack(
                                              listOf(
                                                Text(
                                                  "External LED",
                                                  TextStyle(
                                                    fontSize = 20f,
                                                    color = 0xff213343.toInt()
                                                  )
                                                ),
                                                Padding(
                                                  EdgeInsets.only(left = 4f),
                                                  VStack(
                                                    listOf(
                                                      HStack(
                                                        listOf(
                                                          Text(
                                                            "R",
                                                            TextStyle(
                                                              color = 0xff213343.toInt()
                                                            )
                                                          ),
                                                          Slider(
                                                            { sliderValue },
                                                            onChange = { newValue ->
                                                              sliderValue =
                                                                newValue
                                                            },
                                                            barSize = 320f
                                                          )
                                                        ),
                                                        crossAxisAlignment = StackCrossAxisAlignment.Center
                                                      ),
                                                      /*
                                                      HStack(
                                                        listOf(
                                                          Text(
                                                            "G",
                                                            TextStyle(
                                                              color = 0xff213343.toInt(),
                                                            )
                                                          ),
                                                          Slider(
                                                            { sliderValue },
                                                            onChange = { newValue ->
                                                              sliderValue =
                                                                newValue
                                                            },
                                                            barSize = 320f,
                                                          ),
                                                        ),
                                                        crossAxisAlignment = StackCrossAxisAlignment.Center,
                                                      ), */
                                                      HStack(
                                                        listOf(
                                                          Text(
                                                            "B",
                                                            TextStyle(
                                                              color = 0xff213343.toInt()
                                                            )
                                                          ),
                                                          Slider(
                                                            { sliderValue },
                                                            onChange = { newValue ->
                                                              sliderValue =
                                                                newValue
                                                            },
                                                            barSize = 320f
                                                          )
                                                        ),
                                                        crossAxisAlignment = StackCrossAxisAlignment.Center
                                                      )
                                                    )
                                                  )
                                                )
                                              )
                                            )
                                          ),
                                          fill = 0xffcddae8.toInt()
                                        )
                                      ),
                                      Padding(
                                        EdgeInsets.all(8f),
                                        HSLuvColorPicker(
                                          { _ -> }
                                        )
                                      )
                                    ),
                                    crossAxisAlignment = StackCrossAxisAlignment.Stretch
                                  )
                                ),
                                VirtualBox(width = 8f),
                                Expanded(
                                  VStack(
                                    listOf(
                                      Padding(
                                        EdgeInsets.only(bottom = 4f),
                                        Text("Pico Information", TextStyle(fontSize = 30f))
                                      )
                                    )
                                  )
                                )
                              )
                            )
                          )
                        )
                      )
                    )
                  ),
                  crossAxisAlignment = StackCrossAxisAlignment.Stretch
                )
              )
            )
          )
        }
      }
    )
  }

  override fun draw() {}
}
