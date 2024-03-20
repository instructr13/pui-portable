package dev.wycey.mido.fraiselait.state

import dev.wycey.mido.fraiselait.SerialDevice
import dev.wycey.mido.fraiselait.coroutines.sync.ReadWriteMutex
import kotlinx.coroutines.*

object StateManager {
  private val lock = ReadWriteMutex()
  private var internalState: FramboiseState? = null

  @OptIn(DelicateCoroutinesApi::class)
  private val context = newFixedThreadPoolContext(2, "StateManager")

  private val stateChangeListeners = mutableListOf<(FramboiseState?) -> Unit>()
  private val portsChangeListeners = mutableListOf<(Array<String>) -> Unit>()

  @Volatile
  var shouldInvokeListeners = false

  @Volatile
  var shouldInvokePortsListeners = false

  var state
    get() =
      runBlocking {
        lock.withReadLock {
          internalState
        }
      }
    internal set(value) =
      runBlocking {
        val hasSameValue =
          async {
            lock.withReadLock {
              internalState == value
            }
          }

        if (hasSameValue.await()) {
          return@runBlocking
        }

        lock.withWriteLock {
          internalState = value

          shouldInvokeListeners = true
        }
      }

  init {
    context.use {
      CoroutineScope(it).launch {
        withContext(context = Dispatchers.IO) {
          var lastPorts = emptyArray<String>()

          launch {
            while (true) {
              runBlocking {
                val ports = SerialDevice.list()

                if (lastPorts.contentEquals(ports).not()) {
                  lastPorts = ports

                  shouldInvokePortsListeners = true
                }
              }

              delay(400)
            }
          }
        }
      }
    }
  }

  fun onDrawLoop() {
    if (shouldInvokeListeners) {
      shouldInvokeListeners = false
      val state = state

      stateChangeListeners.forEach { it(state) }
    }

    if (shouldInvokePortsListeners) {
      shouldInvokePortsListeners = false

      portsChangeListeners.forEach { it(SerialDevice.list()) }
    }
  }

  fun addStateChangeListener(listener: (FramboiseState?) -> Unit) {
    stateChangeListeners.add(listener)
  }

  fun addPortsChangeListener(listener: (Array<String>) -> Unit) {
    portsChangeListeners.add(listener)
  }
}
