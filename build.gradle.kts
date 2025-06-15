import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.compose
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import java.io.FileOutputStream
import java.nio.file.Files
import org.jetbrains.compose.reload.ComposeHotRun

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.hotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
}

val javaLanguageVersion = JavaLanguageVersion.of(17)
val linuxArmTarget = "aarch64-unknown-linux-gnu"
val linuxX64Target = "x86_64-unknown-linux-gnu"

// Remember to update Constants.APP_VERSION when changing this version
val projectVersion = "1.5.0-rc02"

val projectName = "Gitnuro"

// Required for JPackage, as it doesn't accept additional suffixes after the version.
val projectVersionSimplified = "1.5.0"

val rustGeneratedSource = "${layout.buildDirectory.get()}/generated/source/uniffi/main/com/jetpackduba/gitnuro/java"

group = "com.jetpackduba"
version = projectVersion

val isLinuxAarch64 = (properties.getOrDefault("isLinuxAarch64", "false") as String).toBoolean()
val useCross = (properties.getOrDefault("useCross", "false") as String).toBoolean()
val isRustRelease = (properties.getOrDefault("isRustRelease", "true") as String).toBoolean()


sourceSets.getByName("main") {
    kotlin.srcDir(rustGeneratedSource)
}

sourceSets.main.get().java.srcDirs("src/main/resources").includes.addAll(arrayOf("**/*.*"))

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {

    when {
        currentOs() == OS.LINUX && isLinuxAarch64 -> implementation(compose.desktop.linux_arm64)
        currentOs() == OS.MAC -> implementation(compose.desktop.macos_x64)
        else -> implementation(compose.desktop.currentOs)
    }

    implementation(compose.uiUtil)
    @OptIn(ExperimentalComposeLibrary::class)
    implementation(compose.desktop.components.animatedImage)
    implementation(compose.components.resources)
    implementation(compose.materialIconsExtended)
    implementation(libs.jgit.core)
    implementation(libs.jgit.gpg)
    implementation(libs.jgit.lfs)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)

    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.reload4j)

    implementation(libs.datastore.core)
    implementation(libs.bouncycastle)

    implementation(libs.ktor.client)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
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

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        allWarningsAsErrors.set(false)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<JavaExec> {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(javaLanguageVersion)
    })
}

//https://github.com/JetBrains/compose-hot-reload
composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}
tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("com.jetpackduba.gitnuro.MainKt")
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
            version = projectVersionSimplified
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
    val archSuffix = if (isLinuxAarch64) {
        "arm_aarch64"
    } else {
        "x86_64"
    }

    archiveBaseName.set("$projectName-linux-$archSuffix-$projectVersion")
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

task("rust_build") {
    buildRust()
}

tasks.getByName("compileKotlin").doLast {
    println("compileKotlin called")
    buildRust()
    copyRustBuild()
    generateKotlinFromRs()
}

tasks.getByName("compileTestKotlin").doLast {
    println("compileTestKotlin called")
    buildRust()
    copyRustBuild()
    generateKotlinFromRs()
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
    generateKotlinFromRs()
}

task("rust_copyBuild") {
    copyRustBuild()
}

fun generateKotlinFromRs() {
    val outDir = "${project.projectDir}/src/main/kotlin/com/jetpackduba/gitnuro/autogenerated/"
    println("Out dir is $outDir")
    val outDirFile = File(outDir)

    if (outDirFile.exists()) {
        outDirFile.listFiles()?.forEach { file -> if (file.name != ".gitignore") file.delete() }
    } else {
        outDirFile.mkdirs()
    }

    // cargo-kotars must be preinstalled
    val command = listOf(
        "cargo-kotars",
        "--kotlin-output",
        outDir,
    )

    exec {
        println("Generating Kotlin source files")

        workingDir = File(project.projectDir, "rs")
        commandLine = command
    }
}

fun buildRust() {
    exec {
        println("Build rs called")
        val binary = if (currentOs() == OS.LINUX && useCross) {
            "cross"
        } else {
            "cargo"
        }

        val params = mutableListOf(
            binary, "build",
        )

        if (isRustRelease) {
            params.add("--release")
        }

        if (currentOs() == OS.LINUX && useCross) {
            if (isLinuxAarch64) {
                params.add("--target=$linuxArmTarget")
            } else {
                params.add("--target=$linuxX64Target")
            }
        } else if (currentOs() == OS.MAC) {
            params.add("--target=x86_64-apple-darwin")
        }

        workingDir = File(project.projectDir, "rs")
        commandLine = params
    }
}

fun copyRustBuild() {
    val outputDir = "${project.projectDir}/src/main/resources"

    val buildTypeDirectory = if (isRustRelease) {
        "release"
    } else {
        "debug"
    }

    val workingDirPath = if (currentOs() == OS.LINUX && useCross) {
        if (isLinuxAarch64) {
            "rs/target/$linuxArmTarget/$buildTypeDirectory"
        } else {
            "rs/target/$linuxX64Target/$buildTypeDirectory"
        }
    } else if (currentOs() == OS.MAC) {
        "rs/target/x86_64-apple-darwin/$buildTypeDirectory"
    } else {
        "rs/target/$buildTypeDirectory"
    }

    val workingDir = File(project.projectDir, workingDirPath)

    val directory = File(outputDir)
    directory.mkdirs()

    val lib = when (currentOs()) {
        OS.LINUX -> "libgitnuro_rs.so"
        OS.WINDOWS -> "gitnuro_rs.dll"
        OS.MAC -> "libgitnuro_rs.dylib"
    }

    val originFile = File(workingDir, lib)
    val destinyFile = File(directory, lib)

    Files.copy(originFile.toPath(), FileOutputStream(destinyFile))

    println("Copy rs build completed")
}
