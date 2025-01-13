package dev.wycey.mido.fraiselait.coroutines.sync

/**
 * Copyright 2017 ModelBox Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class ReadWriteMutexTest {
  private val mutex =
    ReadWriteMutex {
      onStateChange { lastInfo = it }
    }

  private lateinit var lastInfo: MutexInfo

  // Released by test block below
  private val waitOne = Mutex(true)
  private val waitTwo = Mutex(true)

  // Released when the async block starts
  private val startedOne = Mutex(true)
  private val startedTwo = Mutex(true)

  @Test
  fun `test exclusive writes`() {
    runBlocking {
      withTimeout(1.seconds) {
        val one =
          async {
            mutex.withWriteLock {
              startedOne.unlock()
              waitOne.lock()
            }
          }
        val two =
          async {
            mutex.withWriteLock {
              startedTwo.unlock()
              waitTwo.lock()
            }
          }

        // One of the writers will win the write lock and have started, but gotten blocked
        val winner =
          select {
            startedOne.onLock { 1 }
            startedTwo.onLock { 2 }
          }
        assertEquals(MutexInfo(MutexMode.WRITE, MutexState.LOCKED), lastInfo)
        // Unlock the waiters above, and wait for the loser to complete
        when (winner) {
          1 -> {
            assertTrue(startedTwo.isLocked)
            waitOne.unlock()
            waitTwo.unlock()
            two.await()
          }

          2 -> {
            assertTrue(startedOne.isLocked)
            waitOne.unlock()
            waitTwo.unlock()
            one.await()
          }
        }
      }
    }
    assertEquals(MutexInfo(MutexMode.WRITE, MutexState.UNLOCKED), lastInfo)
  }

  @Test
  fun `test read blocks writes`() {
    runBlocking {
      withTimeout(1.seconds) {
        // Start a reader
        val one =
          async {
            mutex.withReadLock {
              startedOne.unlock()
              waitOne.lock()
            }
          }

        // Ensure that the writer has started
        startedOne.lock()

        val beforeWriteLock = Mutex(true)

        // Start a writer
        val two =
          async {
            beforeWriteLock.unlock()
            mutex.withWriteLock {
              startedTwo.unlock()
              waitTwo.lock()
            }
          }

        // Ensure that the writer has started running, but hasn't entered the write block
        beforeWriteLock.lock()
        assertEquals(MutexInfo(MutexMode.READ, MutexState.LOCKED), lastInfo)
        assertFalse(startedTwo.tryLock())

        // Unblock the reader
        waitOne.unlock()
        one.await()

        // Ensure second has entered write block
        startedTwo.lock()
        waitTwo.unlock()
        two.await()
      }
    }
  }

  @Test
  fun `test write blocks reads`() {
    runBlocking {
      withTimeout(1.seconds) {
        val one =
          async {
            mutex.withWriteLock {
              startedOne.unlock()
              waitOne.lock()
            }
          }

        // Ensure that the writer has started
        startedOne.lock()

        val beforeReadLock = Mutex(true)

        // Start a reader
        val two =
          async {
            beforeReadLock.unlock()
            mutex.withReadLock {
              startedTwo.unlock()
              waitTwo.lock()
            }
          }

        // Ensure that the reader has started running, but hasn't entered the read block
        beforeReadLock.lock()
        assertEquals(MutexInfo(MutexMode.WRITE, MutexState.LOCKED), lastInfo)
        assertFalse(startedTwo.tryLock())

        // Unblock the writer
        waitOne.unlock()
        one.await()

        // Ensure second has entered read block
        startedTwo.lock()
        waitTwo.unlock()
        two.await()
      }
    }
  }
}
