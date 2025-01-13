import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("dev.wycey.mido.processing-module")
}

group = "$group.leinwand"

dependencies {
  implementation(libs.colormath)
  implementation(libs.uibooster)

  shadow("dev.wycey.mido.pui:pui")
  shadow("dev.wycey.mido.fraiselait:fraiselait")
}

tasks.withType<ShadowJar> {
  dependencies {
    exclude(dependency(libs.processing.core.get()))
  }
}
