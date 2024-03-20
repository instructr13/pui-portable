package dev.wycey.mido.fraiselait.util

enum class OperatingSystem {
  WINDOWS,
  LINUX,
  MACOS,
  SOLARIS,
  UNKNOWN
}

fun getOperatingSystem(): OperatingSystem {
  val os = System.getProperty("os.name").lowercase()

  return when {
    os.contains("win") -> OperatingSystem.WINDOWS
    os.contains("nix") || os.contains("nux") || os.contains("aix") -> OperatingSystem.LINUX
    os.contains("mac") -> OperatingSystem.MACOS
    os.contains("sunos") -> OperatingSystem.SOLARIS
    else -> OperatingSystem.UNKNOWN
  }
}
