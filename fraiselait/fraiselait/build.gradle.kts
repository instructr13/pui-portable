import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("dev.wycey.mido.processing-module")
}

group = "$group.fraiselait"
version = "2.1.0"

val processingSerial = files("../../lib/org.processing-serial-4.3.jar")

dependencies {
  implementation(libs.kotlinx.coroutines.core)

  shadow(processingSerial)
  shadow(libs.bundles.processing.serial)

  implementation(libs.bundles.jackson)
}

tasks.withType<ShadowJar> {
  dependencies {
    exclude(dependency(libs.processing.core.get()))
  }
}
