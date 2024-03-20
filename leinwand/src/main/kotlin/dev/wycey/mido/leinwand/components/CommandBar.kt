package dev.wycey.mido.leinwand.components

import dev.wycey.mido.leinwand.Styles
import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.input.Button
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.Padding
import dev.wycey.mido.pui.components.text.Text
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.events.mouse.MouseButtons
import dev.wycey.mido.pui.layout.EdgeInsets
import javax.swing.JFileChooser
import javax.swing.JFrame

class CommandBar(private val instanceId: Int) : StatelessComponent("commandBar$instanceId") {
  override fun build(context: BuildContext): Component {
    val handle = dev.wycey.mido.leinwand.LeinwandHandle.instances[instanceId]!!

    return Padding(
      EdgeInsets.symmetric(4f, 8f),
      HStack(
        listOf(
          Button(
            Text("Export as png", Styles.title),
            Styles.topBarButton,
            onClick = onClick@{ _, type ->
              if (type.button != MouseButtons.LEFT) return@onClick

              val frame = JFrame()
              val chooser =
                JFileChooser().apply {
                  dialogTitle = "Save as..."
                  fileSelectionMode = JFileChooser.FILES_ONLY
                  fileFilter =
                    object : javax.swing.filechooser.FileFilter() {
                      override fun accept(f: java.io.File) = f.isDirectory || f.name.endsWith(".png")

                      override fun getDescription() = "PNG files"
                    }
                }

              val userSelection = chooser.showSaveDialog(frame)

              if (userSelection == JFileChooser.APPROVE_OPTION) {
                val fileToSave = chooser.selectedFile

                handle.currentBaseLayer.save(fileToSave.absolutePath)

                handle.statusText = "Saved as ${fileToSave.absolutePath}"
              }
            }
          )
          /*VirtualBox(width = 8f),
          Button(Text("Undo", Styles.title), Styles.topBarButton),
          VirtualBox(width = 8f),
          Button(Text("Redo", Styles.title), Styles.topBarButton),*/
        )
      )
    )
  }
}
