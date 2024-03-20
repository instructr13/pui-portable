import kotlinx.coroutines.runBlocking
import processing.core.PApplet

fun main(vararg args: String) =
  runBlocking {
    PApplet.main(MainApplet::class.java, *args)
  }
