package dev.wycey.mido.pui.bridges

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

interface ProcessingFrameEventHandler {
  fun pre()

  fun draw()

  fun post()
}

interface FrameEventBridgeContract {
  fun registerDrawEventLoop()

  fun onPreDraw(callback: () -> Unit)

  fun onPersistentDraw(callback: () -> Unit)

  fun removePersistentDraw(callback: () -> Unit)

  fun onPostDraw(callback: () -> Unit)
}

class FrameEventBridge internal constructor() : FrameEventBridgeContract,
  ProcessingFrameEventHandler, BridgeBase() {
    companion object {
      @JvmField
      var instanceNullable: FrameEventBridge? = null

      @JvmStatic
      val instance get() = checkInstance(instanceNullable)
    }

    override fun initInstance() {
      super.initInstance()
      instanceNullable = this

      println("Frame event bridge initialized")
    }

    override fun registerDrawEventLoop() {
      applet.registerMethod("pre", this)
      applet.registerMethod("draw", this)
      applet.registerMethod("post", this)
    }

    private val preDrawCallbackQueue = ConcurrentLinkedQueue<() -> Unit>()
    private val persistentDrawCallbacks = CopyOnWriteArrayList<() -> Unit>()
    private val postDrawCallbackQueue = ConcurrentLinkedQueue<() -> Unit>()

    override fun onPreDraw(callback: () -> Unit) {
      preDrawCallbackQueue.add(callback)
    }

    override fun onPersistentDraw(callback: () -> Unit) {
      persistentDrawCallbacks.add(callback)
    }

    override fun removePersistentDraw(callback: () -> Unit) {
      persistentDrawCallbacks.remove(callback)
    }

    override fun onPostDraw(callback: () -> Unit) {
      postDrawCallbackQueue.add(callback)
    }

    private fun invokePreDrawCallbacks() {
      while (!preDrawCallbackQueue.isEmpty()) {
        preDrawCallbackQueue.poll()()
      }
    }

    private fun invokePersistentDrawCallbacks() {
      persistentDrawCallbacks.forEach { it() }
    }

    private fun invokePostDrawCallbacks() {
      while (!postDrawCallbackQueue.isEmpty()) {
        postDrawCallbackQueue.poll()()
      }
    }

    override fun pre() {
      invokePreDrawCallbacks()
    }

    override fun draw() {
      invokePersistentDrawCallbacks()
    }

    override fun post() {
      invokePostDrawCallbacks()
    }
  }
