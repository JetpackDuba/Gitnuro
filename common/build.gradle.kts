plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.kotlin.logging)

    implementation(libs.coroutines)

    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    testImplementation(kotlin("test"))
}
