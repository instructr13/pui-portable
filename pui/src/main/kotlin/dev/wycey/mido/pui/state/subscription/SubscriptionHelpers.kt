package dev.wycey.mido.pui.state.subscription

import dev.wycey.mido.pui.state.signals.Signal

fun <T> wrapWithSubscriptionCallStack(
  subscriptionType: SubscriptionType,
  subscription: () -> T
): () -> T =
  {
    Signal.subscriptionCalls.addLast(subscriptionType)
    subscription().let {
      Signal.subscriptionCalls.removeLast()

      it
    }
  }

fun <T> runWithSubscriptionCallStack(
  subscriptionType: SubscriptionType,
  subscription: () -> T
): T = wrapWithSubscriptionCallStack(subscriptionType, subscription)()
