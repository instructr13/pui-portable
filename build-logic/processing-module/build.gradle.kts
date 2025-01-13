plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(platform("dev.wycey.mido.platform:plugins-platform"))

  implementation(project(":assemble"))

  implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin")
}
