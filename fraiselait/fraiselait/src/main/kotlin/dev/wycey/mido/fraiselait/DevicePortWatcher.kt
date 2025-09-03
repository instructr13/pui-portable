package dev.wycey.mido.fraiselait

import jssc.SerialPortList
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

internal object DevicePortWatcher {
  private val listeners: MutableList<(Array<String>) -> Unit> = mutableListOf()

  var ports: Array<String> = emptyArray()
    private set(value) {
      field = value

      listeners.forEach { it(value) }
    }

  private val job = SupervisorJob()
  private val exceptionHandler =
    CoroutineExceptionHandler { _, throwable ->
      println("An error occurred in DevicePortWatcher:")

      throwable.printStackTrace()

      job.cancel()

      started = false
    }

  @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
  private val coroutineContext = newSingleThreadContext("DevicePortWatcher") + job + exceptionHandler

  private var _started = AtomicBoolean(false)

  var started: Boolean
    get() = _started.get()
    private set(value) {
      _started.set(value)
    }

  fun start() {
    if (started) {
      return
    }

    CoroutineScope(coroutineContext).launch {
      started = true

      while (true) {
        val newSerialList = SerialPortList.getPortNames()

        if (!newSerialList.contentEquals(ports)) {
          ports = newSerialList
        }

        delay(10)
      }
    }
  }

  fun dispose() {
    if (!started) {
      return
    }

    job.cancel()

    started = false
  }

  fun listen(listener: (Array<String>) -> Unit) {
    listeners.add(listener)
  }

  fun unlisten(listener: (Array<String>) -> Unit) {
    listeners.remove(listener)
  }
}
