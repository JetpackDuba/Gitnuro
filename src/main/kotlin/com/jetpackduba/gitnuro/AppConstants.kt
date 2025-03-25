package com.jetpackduba.gitnuro

object AppConstants {
    val openSourceProjects = listOf(
        Project("Google Dagger", "https://dagger.dev/", apache__2_0),
        Project("Compose Multiplatform", "https://www.jetbrains.com/lp/compose-mpp/", apache__2_0),
        Project("JGit", "https://www.eclipse.org/jgit/", edl),
        Project("JUnit 5", "https://junit.org/junit5/", edl),
        Project("Kotlin", "https://kotlinlang.org/", apache__2_0),
        Project(
            "Kotlinx.serialization",
            "https://kotlinlang.org/docs/serialization.html#example-json-serialization",
            apache__2_0
        ),
        Project("Mockk", "https://mockk.io/", apache__2_0),
        Project("Retrofit2", "https://square.github.io/retrofit/", apache__2_0),
        Project("LibSSH", "libssh.org/", lgpl__2_1),
    )


    // Remember to update build.gradle when changing this
    const val APP_NAME = "Gitnuro"
    const val APP_DESCRIPTION =
        "Gitnuro is a Git client that allows you to manage multiple repositories with a modern experience and live visual representation of your repositories' state."
    const val APP_VERSION = "1.5.0-beta02"
    const val APP_VERSION_CODE = 19
    const val VERSION_CHECK_URL = "https://raw.githubusercontent.com/JetpackDuba/Gitnuro/main/latest.json"
}


private val apache__2_0 = License("Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
private val edl = License("EDL", "https://www.eclipse.org/org/documents/edl-v10.php")
private val lgpl__2_1 = License("LGPL-2.1", "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html")

data class License(
    val name: String,
    val url: String,
)

data class Project(val name: String, val url: String, val license: License)


object NetworkConstants {
    const val ACCEPT_HEADER = "Accept"
    const val AUTH_HEADER = "Authorization"
    const val CONTENT_LENGTH_HEADER = "Content-Length"
}