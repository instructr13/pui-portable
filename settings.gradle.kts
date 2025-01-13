rootProject.name = "pui-portable"

val builds = listOf(
  "platforms",
  "build-logic",

  "pui",
  "fraiselait",
  "leinwand",

  "example",
)

builds.forEach { includeBuild(it) }
