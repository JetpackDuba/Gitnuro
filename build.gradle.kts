import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val javaLanguageVersion = JavaLanguageVersion.of(17)

plugins {
    // Kotlin version must match compose version
    kotlin("jvm") version "1.7.10"
    kotlin("kapt") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("org.jetbrains.compose") version "1.5.0"
}

// Remember to update Constants.APP_VERSION when changing this version
val projectVersion = "1.2.1"
val projectName = "Gitnuro"

val rustGeneratedSource = "${buildDir}/generated/source/uniffi/main/com/jetpackduba/gitnuro/java"

group = "com.jetpackduba"
version = projectVersion

sourceSets.getByName("main") {
    kotlin.sourceSets.main.get().kotlin.srcDir(rustGeneratedSource)
}

sourceSets.main.get().java.srcDirs("src/main/resources").includes.addAll(arrayOf("**/*.*"))

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    val jgit = "6.5.0.202303070854-r"

    implementation(compose.desktop.currentOs)
    when (currentOs()) {
        OS.LINUX -> implementation(compose.desktop.linux_arm64) // Include arm for linux builds
        else -> {}
    }

    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.desktop.components.splitPane)
    implementation(compose("org.jetbrains.compose.ui:ui-util"))
    implementation(compose("org.jetbrains.compose.components:components-animatedimage"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgit")
    implementation("org.eclipse.jgit:org.eclipse.jgit.gpg.bc:$jgit")
    implementation("com.google.dagger:dagger:2.45")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    kapt("com.google.dagger:dagger-compiler:2.45")
    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.mockk:mockk:1.13.4")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("net.i2p.crypto:eddsa:0.3.0")
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("io.github.oshai:kotlin-logging-jvm:5.0.1")
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-reload4j:2.0.7")
    implementation("io.arrow-kt:arrow-core:1.2.0")
}

fun currentOs(): OS {
    val os = System.getProperty("os.name")
    return when {
        os.equals("Mac OS X", ignoreCase = true) -> OS.MAC
        os.startsWith("Win", ignoreCase = true) -> OS.WINDOWS
        os.startsWith("Linux", ignoreCase = true) -> OS.LINUX
        else -> error("Unknown OS name: $os")
    }
}

enum class OS {
    LINUX,
    WINDOWS,
    MAC
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

        this@application.dependsOn("rustTasks")

        sourceSets.forEach {
            it.java.srcDir(rustGeneratedSource)
        }

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


task("rust_generateKotlinFromUdl") {
    println("Generate Kotlin")
    generateKotlinFromUdl()
}

task("rust_build") {
    buildRust()
}

tasks.getByName("compileKotlin").doLast {
    println("compileKotlin called")
    buildRust()
    copyRustBuild()
    generateKotlinFromUdl()
}

tasks.getByName("compileTestKotlin").doLast {
    println("compileTestKotlin called")
    buildRust()
    copyRustBuild()
    generateKotlinFromUdl()
}


task("tasksList") {
    println("Tasks")
    tasks.forEach {
        println("- ${it.name}")
    }
}

task("rustTasks") {
    buildRust()
    copyRustBuild()
    generateKotlinFromUdl()
}

task("rust_copyBuild") {
    copyRustBuild()
}

fun generateKotlinFromUdl() {
    exec {
        workingDir = File(project.projectDir, "rs")
        commandLine = listOf(
            "cargo", "run", "--features=uniffi/cli",
            "--bin", "uniffi-bindgen", "generate", "src/repository_watcher.udl",
            "--language", "kotlin",
            "--out-dir", rustGeneratedSource
        )
    }
}

fun buildRust() {
    exec {
        println("Build rs called")
        workingDir = File(project.projectDir, "rs")
        commandLine = listOf(
            "cargo", "build", "--release", "--features=uniffi/cli",
        )
    }
}

fun copyRustBuild() {
    val outputDir = "${buildDir}/classes/kotlin/main"
    println("Copy rs build called")
    val workingDir = File(project.projectDir, "rs/target/release")

    val directory = File(outputDir)
    directory.mkdirs()

    val originLib = when (currentOs()) {
        OS.LINUX -> "libgitnuro_rs.so"
        OS.WINDOWS -> "gitnuro_rs.dll"
        OS.MAC -> "libgitnuro_rs.so" //TODO or is it a dylib? must be tested
    }

    val destinyLib = when (currentOs()) {
        OS.LINUX -> "libuniffi_gitnuro.so"
        OS.WINDOWS -> "uniffi_gitnuro.dll"
        OS.MAC -> "libuniffi_gitnuro.so" //TODO or is it a dylib? must be tested
    }

    val originFile = File(workingDir, originLib)
    val destinyFile = File(directory, destinyLib)

    com.google.common.io.Files.copy(originFile, destinyFile)

    println("Copy rs build completed")
}
