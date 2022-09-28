import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Kotlin version must match compose version
    kotlin("jvm") version "1.7.0"
    kotlin("kapt") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev755"
}

// Remember to update Constants.APP_VERSION when changing this version
val projectVersion = "1.1.0"
val projectName = "Gitnuro"

group = "com.jetpackduba"
version = projectVersion

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.desktop.components.splitPane)
    implementation(compose("org.jetbrains.compose.ui:ui-util"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.2.0.202206071550-r")
    implementation("org.apache.sshd:sshd-core:2.9.0")
    implementation("com.google.dagger:dagger:2.43.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    kapt("com.google.dagger:dagger-compiler:2.43.2")
    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("io.mockk:mockk:1.12.5")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("net.i2p.crypto:eddsa:0.3.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}


compose.desktop {
    application {
        mainClass = "com.jetpackduba.gitnuro.MainKt"

        nativeDistributions {
            includeAllModules = true
            packageName = projectName
            version = projectVersion
            description = "Multiplatform Git client"

            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }

            macOS {
                jvmArgs(
                    "-Dapple.awt.application.appearance=system"
                )
                iconFile.set(project.file("icons/icon.icns"))
            }
        }
    }
}


task("fatJarLinux", type = Jar::class) {
    archiveBaseName.set("$projectName-linux")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Implementation-Title"] = name
        attributes["Implementation-Version"] = projectVersion
        attributes["Main-Class"] = "com.jetpackduba.gitnuro.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude(
            "META-INF/MANIFEST.MF",
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
        )
    }
    with(tasks.jar.get() as CopySpec)
}
