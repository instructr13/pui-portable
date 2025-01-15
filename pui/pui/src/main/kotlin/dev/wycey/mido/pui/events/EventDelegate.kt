package dev.wycey.mido.pui.events

internal open class EventDelegate<T : Function<*>> : Iterable<T> {
  private val events = mutableListOf<T>()

  override fun iterator(): Iterator<T> {
    return events.iterator()
  }

  operator fun plusAssign(event: T) {
    events.add(event)
  }

  operator fun minusAssign(event: T) {
    events.remove(event)
  }
}
