import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val javaLanguageVersion = JavaLanguageVersion.of(17)

plugins {
    // Kotlin version must match compose version
    kotlin("jvm") version "1.7.10"
    kotlin("kapt") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("org.jetbrains.compose") version "1.3.0-rc01"
}

// Remember to update Constants.APP_VERSION when changing this version
val projectVersion = "1.1.1"
val projectName = "Gitnuro"

group = "com.jetpackduba"
version = projectVersion

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    val jgit = "6.4.0.202211300538-r"

    implementation(compose.desktop.currentOs)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.desktop.components.splitPane)
    implementation(compose("org.jetbrains.compose.ui:ui-util"))
    implementation(compose("org.jetbrains.compose.components:components-animatedimage"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgit")
    implementation("org.eclipse.jgit:org.eclipse.jgit.gpg.bc:$jgit")
    implementation("org.apache.sshd:sshd-core:2.9.0")
    implementation("com.google.dagger:dagger:2.44.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    kapt("com.google.dagger:dagger-compiler:2.44.2")
    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("io.mockk:mockk:1.13.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("net.i2p.crypto:eddsa:0.3.0")
    implementation("net.java.dev.jna:jna:5.13.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.withType<JavaExec> {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(javaLanguageVersion)
    })
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