package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.extensions.OS
import com.jetpackduba.gitnuro.extensions.getCurrentOs
import com.jetpackduba.gitnuro.logging.printError
import java.io.File
import javax.inject.Inject

private const val TAG = "AppFilesManager"

class AppFilesManager @Inject constructor() {
    fun getAppFolder(): File {
        val baseFolderPath = when (getCurrentOs()) {
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