import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // __KOTLIN_COMPOSE_VERSION__
    kotlin("jvm") version "1.5.31"
    kotlin("kapt") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
    // __LATEST_COMPOSE_RELEASE_VERSION__
    id("org.jetbrains.compose") version "1.0.0-beta6-dev464"
}

group = "aeab13.github"
version = "1.0"

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}



dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    implementation("org.apache.sshd:sshd-core:2.7.0")
    implementation("com.google.dagger:dagger:2.39.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    kapt("com.google.dagger:dagger-compiler:2.39.1")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "gitnuro"
            packageVersion = "1.0.0"
        }
    }
}