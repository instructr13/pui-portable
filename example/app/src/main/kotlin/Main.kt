import kotlinx.coroutines.runBlocking
import processing.core.PApplet

public fun main(vararg args: String): Unit =
  runBlocking {
    PApplet.main(MainApplet::class.java, *args)
  }
