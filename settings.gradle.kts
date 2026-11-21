pluginManagement {
    repositories {
        maven { url = uri("https://raw.githubusercontent.com/graalvm/native-build-tools/snapshots") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://repo.eclipse.org/content/groups/releases/") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

rootProject.name = "Gitnuro"
include(":ui")
include(":app")
include(":domain")
include(":data")
include(":common")
