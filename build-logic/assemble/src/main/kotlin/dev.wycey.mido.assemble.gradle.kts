import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gradle.kotlin.dsl.accessors._c96b7a83f86a5040182983aa148b3c68.assemble
import gradle.kotlin.dsl.accessors._c96b7a83f86a5040182983aa148b3c68.implementation

plugins {
  id("dev.wycey.mido.commons")
  id("com.gradleup.shadow")
}

// Issue: https://github.com/GradleUp/shadow/issues/448
tasks.withType<ShadowJar> {
  configurations = listOf(
    project.configurations.implementation.get(),
  ).onEach { it.isCanBeResolved = true }
}

tasks.assemble {
  finalizedBy("shadowJar")
}
