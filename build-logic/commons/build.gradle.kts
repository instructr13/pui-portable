plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(platform("dev.wycey.mido.platform:plugins-platform"))

  implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin")
  implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin")
}
