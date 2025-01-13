plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(platform("dev.wycey.mido.platform:plugins-platform"))

  implementation(project(":commons"))

  implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin")
}
