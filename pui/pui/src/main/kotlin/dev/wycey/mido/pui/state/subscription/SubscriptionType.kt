package dev.wycey.mido.pui.state.subscription

import dev.wycey.mido.pui.state.signals.Subscriber

public sealed class SubscriptionType {
  public data object Root : SubscriptionType()

  public data object Effect : SubscriptionType()

  public data class Batch(
    val depth: Int = 0,
    val updates: MutableList<() -> Unit> = mutableListOf(),
    val effects: MutableSet<Subscriber> = mutableSetOf()
  ) : SubscriptionType()

  public data object Untracked : SubscriptionType()
}
