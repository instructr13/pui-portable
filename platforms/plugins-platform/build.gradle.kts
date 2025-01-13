plugins {
  id("java-platform")
}

group = "dev.wycey.mido.platform"

dependencies {
  constraints {
    api(libs.gradlePlugins.kotlin)
    api(libs.gradlePlugins.ktlint)
    api(libs.gradlePlugins.shadow)
  }
}
