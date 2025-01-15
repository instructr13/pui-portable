@file:JvmName("Effect")

package dev.wycey.mido.pui.state.signals

import dev.wycey.mido.pui.state.computation.Computation
import dev.wycey.mido.pui.state.computation.createComputation
import dev.wycey.mido.pui.state.subscription.SubscriptionType
import dev.wycey.mido.pui.state.subscription.runWithSubscriptionCallStack
import dev.wycey.mido.pui.state.subscription.wrapWithSubscriptionCallStack

public fun effect(subscriber: () -> Unit): Unsubscribe =
  createComputation(
    subscriber,
    wrapWithSubscriptionCallStack(SubscriptionType.Effect, subscriber)
  )::dispose

public fun <T : Any> effect(subscriber: (previous: T?) -> T): Unsubscribe =
  createComputation(
    subscriber
  ) {
    runWithSubscriptionCallStack(SubscriptionType.Effect) {
      subscriber(it)
    }
  }::dispose

public fun <T> untracked(f: () -> T): T = runWithSubscriptionCallStack(SubscriptionType.Untracked, f)

public fun onDisposeEffect(f: () -> Unit) {
  if (Signal.lastSubscriptionCall is SubscriptionType.Effect) {
    return
  }

  if (Computation.lastComputation != null) {
    Computation.lastComputation?.onDispose(f)

    return
  }

  throw IllegalStateException("onDisposeEffect must be called inside an effect")
}
