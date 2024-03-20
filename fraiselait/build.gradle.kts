dependencies {
  implementation(libs.kotlinx.coroutines.core)

  implementation(files("../lib/org.processing-serial-4.3.jar"))
  implementation(libs.bundles.processing.serial)

  implementation(libs.bundles.jackson)
}
