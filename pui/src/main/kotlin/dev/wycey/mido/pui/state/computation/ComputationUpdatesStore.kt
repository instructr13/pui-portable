package dev.wycey.mido.pui.state.computation

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class ComputationUpdatesStore {
  private val lock = ReentrantReadWriteLock()
  private val readLock = lock.readLock()
  private val writeLock = lock.writeLock()

  private val inner = ArrayDeque<() -> Unit>()

  fun add(update: () -> Unit) =
    writeLock.withLock {
      inner.addLast(update)
    }

  fun runAll() =
    readLock.withLock {
      while (inner.isNotEmpty()) {
        inner.removeFirst()()
      }
    }
}
