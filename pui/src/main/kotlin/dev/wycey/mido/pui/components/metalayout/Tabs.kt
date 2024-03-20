package dev.wycey.mido.pui.components.metalayout

import dev.wycey.mido.pui.components.base.Component
import dev.wycey.mido.pui.components.basic.StatelessComponent
import dev.wycey.mido.pui.components.layout.HStack
import dev.wycey.mido.pui.components.layout.VStack
import dev.wycey.mido.pui.components.layout.ZStack
import dev.wycey.mido.pui.elements.base.BuildContext
import dev.wycey.mido.pui.layout.Offstage

class Tabs
  @JvmOverloads
  constructor(
    private val tabs: List<Pair<String, Component?>>,
    private val selectedTabIndex: Int,
    private val onTabSelected: (Int) -> Unit,
    key: String? = null
  ) : StatelessComponent(key) {
    override fun build(context: BuildContext): Component {
      val tabComponents =
        tabs.mapIndexed { index, tab ->
          TabItem(
            tab.first,
            index == selectedTabIndex,
            onSelect = { onTabSelected(index) },
            tab.second == null,
            "tab-$index-${index == selectedTabIndex}"
          )
        }

      return VStack(
        listOf(
          HStack(tabComponents),
          ZStack(
            tabs.mapIndexed { index, tab ->
              Offstage(tab.second, index != selectedTabIndex)
            }
          )
        )
      )
    }
  }
