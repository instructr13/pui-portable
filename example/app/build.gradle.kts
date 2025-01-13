plugins {
  id("dev.wycey.mido.processing-module")
  application
}

group = "$group.app"

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.colormath)

  implementation(libs.processing.core)

  implementation("dev.wycey.mido.pui:pui")
}

application {
  mainClass = "MainKt"
}
