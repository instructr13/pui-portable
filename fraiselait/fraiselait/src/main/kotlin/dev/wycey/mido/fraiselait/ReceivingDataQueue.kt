package dev.wycey.mido.fraiselait

import dev.wycey.mido.fraiselait.coroutines.sync.newCondition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

internal class ReceivingDataQueue {
  private val lock = Mutex()
  private val innerHasDataCondition = lock.newCondition()
  private val inner = ArrayDeque<ByteArray>()

  suspend fun push(data: ByteArray) =
    lock.withLock {
      inner.add(data)

      innerHasDataCondition.signal()
    }

  @OptIn(ExperimentalTime::class)
  suspend fun pop() =
    lock.withLock {
      val ok = innerHasDataCondition.awaitUntil(10.seconds) { inner.isNotEmpty() }

      if (!ok) {
        throw RuntimeException("Timeout waiting for data in ReceivingDataQueue")
      }

      inner.removeFirst()
    }

  suspend fun peek() =
    lock.withLock {
      inner.firstOrNull()
    }

  suspend fun isEmpty() =
    lock.withLock {
      inner.isEmpty()
    }

  suspend fun clear() =
    lock.withLock {
      inner.clear()
    }
}
