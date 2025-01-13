pluginManagement {
  repositories {
    gradlePluginPortal()
  }

  includeBuild("../build-logic")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()

    maven(url = "https://jitpack.io")
    maven(url = "https://jogamp.org/deployment/maven")
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

includeBuild("../platforms")

includeBuild("../pui")
includeBuild("../fraiselait")

rootProject.name = "leinwand"

include("leinwand")
