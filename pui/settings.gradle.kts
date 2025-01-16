pluginManagement {
  repositories {
    gradlePluginPortal()
  }

  includeBuild("../build-logic")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()

    maven(url = "https://jogamp.org/deployment/maven")
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

includeBuild("../platforms")

rootProject.name = "pui"

include("pui")
