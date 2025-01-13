import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("dev.wycey.mido.processing-module")
}

group = "$group.pui"

dependencies {
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.colormath)
}

tasks.withType<ShadowJar> {
  dependencies {
    exclude(dependency(libs.processing.core.get()))
  }
}
