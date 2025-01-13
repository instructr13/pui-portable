package dev.wycey.mido.pui.state.subscription

import dev.wycey.mido.pui.state.signals.Subscriber

sealed class SubscriptionType {
  data object Root : SubscriptionType()

  data object Effect : SubscriptionType()

  data class Batch(
    val depth: Int = 0,
    val updates: MutableList<() -> Unit> = mutableListOf(),
    val effects: MutableSet<Subscriber> = mutableSetOf()
  ) :
    SubscriptionType()

  data object Untracked : SubscriptionType()
}
