import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.jvm.optionals.getOrNull

plugins {
  id("java")
  kotlin("jvm")
  id("org.jlleitschuh.gradle.ktlint")
}

group = "dev.wycey.mido"
version = "1.0.0"

val libs = versionCatalogs.catalogNames.firstNotNullOfOrNull { versionCatalogs.find(it).getOrNull() }

dependencies {
  if (libs != null) {
    // global libraries area
  }

  testImplementation(kotlin("test"))
}

dependencyLocking {
  lockAllConfigurations()
}

java {
  withSourcesJar()
}

kotlin {
  explicitApi()

  jvmToolchain(17)
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

tasks.test {
  useJUnitPlatform()
}
