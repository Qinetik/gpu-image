pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        id("org.jetbrains.compose").version(extra["compose.jb.version"] as String).apply(false)
    }
}
rootProject.name = "KGpuImage"
include(":library")
include(":sample")
include(":gpu-image")
include(":web")
include(":desktop")