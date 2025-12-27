package com.jetpackduba.gitnuro.credentials.external

import java.io.File
import javax.inject.Inject

class WindowsGitCredentialsManagerProvider @Inject constructor() : IGitCredentialsManagerProvider {
    override fun loadPath(): String? {
        return lookForGitCredentialsManager(
            listOf(
                "git-credential-manager.exe",
                "git-credential-manager-core.exe"
            )
        )
    }

    fun lookForGitCredentialsManager(binariesNames: List<String>): String? {
        val credentialManagerBinary = searchForBinariesInPath(binariesNames)

        if (credentialManagerBinary != null) {
            return credentialManagerBinary
        }

        val git = searchForBinariesInPath(listOf("git.exe"))

        if (git != null) {
            val gitFile = File(git)
            val baseDir = gitFile.parentFile?.parentFile

            if (baseDir != null) {
                if (baseDir.name.startsWith("mingw")) {
                    val mingwBinaries = File(baseDir, "bin")
                    val gitCredentialsBinary = File(mingwBinaries, "git-credential-manager.exe")

                    return if (gitCredentialsBinary.exists()) {
                        println("Git Credentials binary 1: ${gitCredentialsBinary.absolutePath}")
                        gitCredentialsBinary.absolutePath
                    } else {
                        println("Git Credentials binary 1 not found")
                        null
                    }
                } else {
                    val mingwDirs = baseDir.listFiles()
                        .orEmpty()
                        .filter { it.name.startsWith("mingw") }

                    for (mingwDir in mingwDirs) {
                        val mingwBinaries = File(mingwDir, "bin")
                        val gitCredentialsBinary = File(mingwBinaries, "git-credential-manager.exe")

                        if (gitCredentialsBinary.exists()) {
                            println("Git Credentials binary 2: ${gitCredentialsBinary.absolutePath}")
                            return gitCredentialsBinary.absolutePath
                        } else {
                            println("Git Credentials binary 2 not found")
                            return null
                        }
                    }

                    return null
                }
            } else {
                return null
            }
        } else {
            return null
        }
    }

    fun searchForBinariesInPath(binariesNames: List<String>): String? {
        val path = System.getenv("PATH")
        val pathDirs = path.split(";")

        for (dir in pathDirs) {
            for (names in binariesNames) {
                val file = File(dir, names)
                if (file.exists() && file.isFile) {
                    return file.absolutePath
                }
            }
        }

        return null
    }
}