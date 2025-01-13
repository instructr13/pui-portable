package dev.wycey.mido.pui.events

open class EventArgs protected constructor() {
  companion object {
    @JvmField
    val EMPTY = EventArgs()
  }
}
