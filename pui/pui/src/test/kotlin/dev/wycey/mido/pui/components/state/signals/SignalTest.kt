package dev.wycey.mido.pui.components.state.signals

import dev.wycey.mido.pui.state.signals.*
import dev.wycey.mido.pui.state.signals.context.createRootSignalContext
import dev.wycey.mido.pui.state.signals.context.runRootContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SignalTest {
  private fun mockRootContext(f: () -> Unit) {
    val rootContext = createRootSignalContext {}

    runRootContext(rootContext) {
      f()
    }
  }

  @Test
  fun `test simple signal`() {
    var count: Int by signal(0)

    assertEquals(0, count)

    count = 1

    assertEquals(1, count)
  }

  @Test
  fun `test effect`() {
    mockRootContext {
      var count: Int by signal(0)
      var effectCount = 0
      val f: (Int) -> Unit = {}

      effect {
        f(count)

        effectCount++
      }

      assertEquals(1, effectCount)

      count = 1

      assertEquals(2, effectCount)
    }
  }

  @Test
  fun `test untracked peek`() {
    mockRootContext {
      var count: Int by signal(0)
      var effectCount = 0
      val f: (Int) -> Unit = {}

      effect {
        f(untracked { count })

        effectCount++
      }

      assertEquals(1, effectCount)

      count = 1

      assertEquals(1, effectCount)
    }
  }

  @Test
  fun `test untracked callback`() {
    mockRootContext {
      var count: Int by signal(0)
      var effectCount: Int by signal(0)
      val f: (Int) -> Unit = {}
      val apply = { effectCount + 1 }

      effect {
        f(count)

        effectCount = untracked(apply)
      }

      assertEquals(1, effectCount)

      count = 1

      assertEquals(2, effectCount)
    }
  }

  @Test
  fun `test computed`() {
    mockRootContext {
      var name by signal("John")
      val surname by signal("Doe")

      val fullName by computed { "$name $surname" }

      assertEquals("John Doe", fullName)

      name = "Jane"

      assertEquals("Jane Doe", fullName)
    }
  }

  @Test
  fun `test computed effect`() {
    mockRootContext {
      var name by signal("John")
      val surname by signal("Doe")

      val fullName by computed { "$name $surname" }

      val f: (String) -> Unit = {}

      var effectCount = 0

      effect {
        f(fullName)

        effectCount++
      }

      assertEquals(1, effectCount)

      name = "Jane"

      assertEquals(2, effectCount)
    }
  }

  @Test
  fun `test effect dispose`() {
    mockRootContext {
      var count by signal(0)
      var effectCount = 0
      var disposed = false
      var disposed2 = false
      val f: (Int) -> Unit = {}

      val dispose =
        effect {
          f(count)

          onDisposeEffect {
            disposed = true
          }

          onDisposeEffect {
            disposed2 = true
          }

          effectCount++
        }

      assertEquals(1, effectCount)

      count = 1

      assertEquals(2, effectCount)

      dispose()

      assertEquals(true, disposed)
      assertEquals(true, disposed2)
      assertEquals(2, effectCount)

      count = 2

      assertEquals(2, effectCount)
    }
  }

  @Test
  fun `test batch`() {
    mockRootContext {
      var count by signal(0)
      var effectCount = 0
      val f: (Int) -> Unit = {}

      effect {
        f(count)

        effectCount++
      }

      assertEquals(1, effectCount)

      batch {
        count = 1
        count = 2
        count = 3
      }

      assertEquals(2, effectCount)
    }
  }
}
