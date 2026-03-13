pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }

}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}
rootProject.name = "Gitnuro"
