import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ktlint)
}

allprojects {
  group = "dev.wycey.mido"
  version = "1.0.0"

  repositories {
    mavenCentral()

    maven(url = "https://jitpack.io")
    maven(url = "https://jogamp.org/deployment/maven")
  }

  apply {
    plugin(rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    plugin(rootProject.libs.plugins.ktlint.get().pluginId)
  }

  dependencies {
    implementation(rootProject.libs.processing.core)

    testImplementation(rootProject.libs.kotlin.test)
  }

  dependencyLocking {
    lockAllConfigurations()
  }

  configurations {
    compileClasspath {
      resolutionStrategy.activateDependencyLocking()
    }

    testCompileClasspath {
      resolutionStrategy.activateDependencyLocking()
    }
  }

  tasks.test {
    useJUnitPlatform()
  }

  afterEvaluate {
    tasks.withType<Jar> {
      enabled = true
      isZip64 = true
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE

      from(sourceSets.main.get().output)
      dependsOn(configurations.compileClasspath)

      from({
        configurations.compileClasspath.get().filter {
          it.name.endsWith("jar") &&
            !it.name.contains("processing-core") &&
            !it.name.contains("processing-serial") &&
            !it.name.contains("slf4j")
        }.map { zipTree(it) }
      }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
      }
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    options.release = 17
  }

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
  }

  java {
    withSourcesJar()
  }

  kotlin {
    jvmToolchain(17)
  }
}

subprojects {
  val buildDir = rootProject.layout.buildDirectory.get().asFile.absolutePath

  layout.buildDirectory = file("$buildDir/$name")

  tasks.withType<Jar> {
    destinationDirectory = file("$buildDir/libs")
  }
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.processing.core)

  implementation(project(":pui"))
}

tasks.withType<Jar> {
  enabled = true
  isZip64 = true
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  from(sourceSets.main.get().output)
  dependsOn(configurations.compileClasspath)

  from({
    configurations.compileClasspath.get().filter {
      it.name.endsWith("jar")
    }.map { zipTree(it) }
  }) {
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
  }

  manifest {
    attributes["Main-Class"] = "MainKt"
  }
}
