import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("dev.wycey.mido.assemble")
}

val catalog = versionCatalogs.named("libs")

dependencies {
  implementation(catalog.findLibrary("processing-core").get())
}

tasks.withType<ShadowJar> {
  dependencies {
    exclude {
      it.moduleGroup == "org.slf4j"
    }
  }
}
