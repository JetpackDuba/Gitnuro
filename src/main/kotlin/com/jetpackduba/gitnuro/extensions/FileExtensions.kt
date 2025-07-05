package com.jetpackduba.gitnuro.extensions

import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.system.OS
import com.jetpackduba.gitnuro.system.currentOs
import kotlinx.io.IOException
import java.awt.Desktop
import java.io.File

private const val TAG = "FileExtensions"

fun File.openDirectory(dirName: String): File {
    val newDir = File(this, dirName)

    if (!newDir.exists() || !newDir.isDirectory) {
        newDir.mkdirs()
    }

    return newDir
}

fun File.openFileInFolder() {

    if (!exists() || !isDirectory) {
        printError(TAG, "Folder with path $path does not exist or is not a folder")
        return
    }

    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(this)
                return
            }
        }
    } catch (e: Exception) {
        printError(TAG, "Desktop API failed: ${e.message}")
    }

    // Fallback
    val os = currentOs
    val command = when (os) {
        OS.LINUX -> listOf("xdg-open", absolutePath)
        OS.MAC -> listOf("open", absolutePath)
        OS.WINDOWS -> listOf("explorer", absolutePath)
        else -> null
    }

    if (command != null) {
        try {
            ProcessBuilder(command).start()
        } catch (ex: IOException) {
            printError(TAG, "Failed to open file explorer: ${ex.message}")
        }
    } else {
        printError(TAG, "Unsupported OS: $os")
    }
}