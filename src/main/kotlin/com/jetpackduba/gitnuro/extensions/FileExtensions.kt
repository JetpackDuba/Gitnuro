package com.jetpackduba.gitnuro.extensions

import kotlinx.io.IOException
import java.awt.Desktop
import java.io.File
import java.util.*

fun File.openDirectory(dirName: String): File {
    val newDir = File(this, dirName)

    if (!newDir.exists() || !newDir.isDirectory) {
        newDir.mkdirs()
    }

    return newDir
}

fun File.openFileInFolder() {

    if (!exists() || !isDirectory) {
        println("Folder with path $path does not exist or is not a folder")
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
        println("Desktop API failed: ${e.message}")
    }

    // Fallback
    val os = System.getProperty("os.name").lowercase(Locale.getDefault())
    val command = when {
        os.contains("linux") -> listOf("xdg-open", absolutePath)
        os.contains("mac") -> listOf("open", absolutePath)
        os.contains("windows") -> listOf("explorer", absolutePath)
        else -> null
    }

    if (command != null) {
        try {
            ProcessBuilder(command).start()
        } catch (ex: IOException) {
            println("Failed to open file explorer: ${ex.message}")
        }
    } else {
        println("Unsupported OS: $os")
    }
}