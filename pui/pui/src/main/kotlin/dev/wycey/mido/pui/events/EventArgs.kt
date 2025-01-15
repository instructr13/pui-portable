package dev.wycey.mido.pui.events

public open class EventArgs protected constructor() {
  public companion object {
    @JvmField
    public val EMPTY: EventArgs = EventArgs()
  }
}
