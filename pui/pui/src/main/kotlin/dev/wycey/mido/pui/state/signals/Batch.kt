@file:JvmName("Batch")

package dev.wycey.mido.pui.state.signals

import dev.wycey.mido.pui.state.subscription.SubscriptionType
import dev.wycey.mido.pui.state.subscription.runWithSubscriptionCallStack

public fun batch(f: () -> Unit) {
  val depth = Signal.lastSubscriptionCall?.let { it as? SubscriptionType.Batch }?.depth ?: 0

  if (depth > 0) {
    f()

    return
  }

  val batch = SubscriptionType.Batch(depth + 1)

  runWithSubscriptionCallStack(
    batch
  ) {
    f()
  }

  batch.apply {
    updates.forEach { it() }
    effects.forEach { it() }
  }
}
