package com.jetpackduba.gitnuro.domain

import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.extensions.openDirectory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TempFilesManager @Inject constructor(
    private val appFilesManager: AppFilesManager,
) {
    fun tempDir(): File {
        val appDataDir = appFilesManager.getAppFolder()
        return appDataDir.openDirectory("tmp")
    }

    fun clearAll() {
        val dir = tempDir()
        dir.deleteRecursively()
    }
}

private const val TAG = "AppFilesManager"

class AppFilesManager @Inject constructor() {
    fun getAppFolder(): File {
        val baseFolderPath = when (currentOs) {
            OS.LINUX -> {
                var configFolder: String? = System.getenv("XDG_CONFIG_HOME")

                if (configFolder.isNullOrEmpty())
                    configFolder = "${System.getenv("HOME")}/.config"


                configFolder
            }

            OS.WINDOWS -> System.getenv("APPDATA").orEmpty()
            OS.MAC -> System.getProperty("user.home") + "/Library/Application"
            else -> {
                printError(TAG, "Unknown OS")
                throw Exception("Invalid OS")
            }
        }

        val baseFolder = File(baseFolderPath)

        baseFolder.mkdirs()

        val appFolder = File(baseFolder, "gitnuro")
        // TODO test if mkdir fails for some reason
        if (!appFolder.exists() || !appFolder.isDirectory)
            appFolder.mkdir()

        return appFolder
    }
}