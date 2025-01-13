dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

includeBuild("../platforms")

rootProject.name = "build-logic"

include("commons", "assemble", "processing-module")
