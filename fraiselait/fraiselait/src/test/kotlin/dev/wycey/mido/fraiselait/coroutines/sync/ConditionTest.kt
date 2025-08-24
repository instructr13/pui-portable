/**
 * Source: https://gist.github.com/paulo-raca/ef6a827046a5faec95024ff406d3a692
 */

package dev.wycey.mido.fraiselait.coroutines.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ConditionTest {
  val lock = Mutex()
  internal val cond = lock.newCondition()

  @Test
  fun testAwaitWithoutSignal() {
    runBlocking {
      lock.withLock {
        Assertions.assertFalse(cond.await(1.seconds))
      }
    }
  }

  @Test
  fun testAwaitSignal() {
    runBlocking {
      launch {
        delay(500)
        lock.withLock {
          cond.signal()
        }
      }
      lock.withLock {
        Assertions.assertTrue(cond.await(1.seconds))
        Assertions.assertFalse(cond.await(1.seconds))
      }
    }
  }

  @Test
  fun testSignalAwait() {
    runBlocking {
      lock.withLock {
        cond.signal()
      }
      lock.withLock {
        delay(500)
        Assertions.assertFalse(cond.await(1.seconds))
      }
    }
  }

  @Test
  fun testNotifyOnce() {
    runBlocking {
      val waiters =
        IntStream
          .range(0, 5)
          .mapToObj {
            async {
              lock.withLock {
                val ret = cond.await(1.seconds)
                ret
              }
            }
          }.collect(Collectors.toList())
          .toTypedArray()

      delay(100.milliseconds)
      lock.withLock {
        cond.signal()
      }
      val results = awaitAll(*waiters)
      val successCount =
        results
          .stream()
          .map { ret -> if (ret) 1 else 0 }
          .reduce { a, b -> a + b }
          .get()
      Assertions.assertEquals(1, successCount)
    }
  }

  @Test
  fun testNotifyAll() {
    runBlocking {
      val waiters =
        IntStream
          .range(0, 5)
          .mapToObj {
            async {
              lock.withLock {
                val ret = cond.await(1.seconds)
                ret
              }
            }
          }.collect(Collectors.toList())
          .toTypedArray()

      delay(100.milliseconds)
      lock.withLock {
        cond.signalAll()
      }
      val results = awaitAll(*waiters)
      val successCount =
        results
          .stream()
          .map { ret -> if (ret) 1 else 0 }
          .reduce { a, b -> a + b }
          .get()
      Assertions.assertEquals(results.size, successCount)
    }
  }
}
