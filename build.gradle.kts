addDependencyOnChildTasksOfIncludedBuilds("assemble", "build", "clean", "check")

defaultTasks("run")

tasks.register("run") {
  dependsOn(gradle.includedBuild("example").task(":app:run"))
}

tasks.register("test") {
  dependsOn(gradle.includedBuild("pui").task(":pui:test"))
  dependsOn(gradle.includedBuild("fraiselait").task(":fraiselait:test"))
}

fun filteredIncludedBuilds(filter: (IncludedBuild) -> Boolean = { true }) =
  gradle.includedBuilds
    .filter { it.name != "build-logic" && it.name != "platforms" }
    .filter(filter)

fun addDependencyOnChildTasksOfIncludedBuilds(vararg taskNames: String) {
  taskNames.forEach { taskName ->
    tasks.register(taskName) {
      dependsOn(filteredIncludedBuilds().flatMap {
        it.projectDir.walkTopDown().filter { it.name == "build.gradle.kts" }.map { file ->
          val path = file.parentFile.relativeTo(it.projectDir).path

          it.task(":$path:$taskName")
        }
      })
    }
  }
}
