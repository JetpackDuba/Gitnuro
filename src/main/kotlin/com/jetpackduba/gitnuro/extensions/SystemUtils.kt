package com.jetpackduba.gitnuro.extensions

import com.jetpackduba.gitnuro.logging.printLog
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.FileSystems

private const val TAG = "SystemUtils"

val systemSeparator: String by lazy {
    FileSystems.getDefault().separator
}

fun openUrlInBrowser(url: String) {
    if (!openSystemSpecific(url)) {
        openUrlInBrowserJdk(url)
    }
}

fun openFileWithExternalApp(filePath: String) {
    if (!openSystemSpecific(filePath)) {
        openFileJdk(filePath)
    }
}

private fun openSystemSpecific(url: String): Boolean {
    when (getCurrentOs()) {
        OS.LINUX -> {
            if (runCommandWithoutResult("xdg-open", "%s", url))
                return true
            if (runCommandWithoutResult("kde-open", "%s", url))
                return true
            if (runCommandWithoutResult("gnome-open", "%s", url))
                return true
        }

        OS.WINDOWS -> if (runCommandWithoutResult("explorer", "%s", url)) return true
        OS.MAC -> if (runCommandWithoutResult("open", "%s", url)) return true
        else -> printLog(TAG, "Unknown OS")
    }

    return false
}

private fun openUrlInBrowserJdk(url: String) {

    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (ex: Exception) {
        println("Failed to open URL in browser")
        ex.printStackTrace()
    }
}

private fun openFileJdk(filePath: String) {
    try {
        Desktop.getDesktop().open(File(filePath))
    } catch (ex: Exception) {
        println("Failed to open URL in browser")
        ex.printStackTrace()
    }
}

enum class OS {
    LINUX,
    WINDOWS,
    MAC,
    UNKNOWN;

    fun isLinux() = this == LINUX

    fun isWindows() = this == WINDOWS

    fun isMac() = this == MAC
}

fun getCurrentOs(): OS {
    val os = System.getProperty("os.name").lowercase()
    printLog(TAG, "OS is $os")

    return when {
        os.contains("linux") -> OS.LINUX
        os.contains("windows") -> OS.WINDOWS
        os.contains("mac") -> OS.MAC
        else -> OS.UNKNOWN
    }
}