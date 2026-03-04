plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(project(":common"))

    // TODO Remove this after refactor
    implementation(compose.desktop.currentOs)

    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    implementation(libs.coroutines)

    // TODO This should be removed after refactor is finished
    implementation(libs.jgit.core)
    implementation(libs.jgit.gpg)
    implementation(libs.jgit.lfs)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.mockk)

    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.reload4j)

    implementation(libs.bouncycastle)

    implementation(libs.ktor.client)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

}
