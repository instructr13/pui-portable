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

    // Required for the ui-booster library
    maven(url = "https://jitpack.io")
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
