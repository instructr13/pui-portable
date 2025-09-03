plugins {
  id("dev.wycey.mido.processing-module")
}

group = "$group.fraiselait"
version = "2.2.0"

dependencies {
  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.jssc)
}
