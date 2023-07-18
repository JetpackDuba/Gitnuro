package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.extensions.openDirectory
import com.jetpackduba.gitnuro.system.OS
import com.jetpackduba.gitnuro.system.getCurrentOs
import org.apache.log4j.*
import java.io.File

fun initLogging() {
    val layout = PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n")

    val filePath = when (getCurrentOs()) {
        OS.LINUX -> linuxLogsPathAppender()
        OS.WINDOWS -> windowsLogsPathAppender()
        OS.MAC -> macosLogsPath()
        OS.UNKNOWN -> defaultLogsPath()
    }

    val fileAppender = RollingFileAppender(layout, filePath, true)
    fileAppender.maximumFileSize = 10 * 1024 * 1024 // 10MB
    fileAppender.maxBackupIndex = 5

    val consoleAppender = ConsoleAppender(layout)

    LogManager.getRootLogger().apply {
        addAppender(fileAppender)
        addAppender(consoleAppender)
        level = Level.INFO
    }
}

private fun defaultLogsPath(): String {
    val homePath = System.getProperty("user.home").orEmpty()

    return "$homePath/gitnuro/gitnuro.logs"
}

private fun macosLogsPath(): String {
    val logsDir = File(System.getProperty("user.home") + "/Library/Logs/")
        .openDirectory("com.jetpackduba.Gitnuro")

    return "${logsDir.absolutePath}/gitnuro.log"
}

private fun windowsLogsPathAppender(): String {
    val localAppData = System.getenv("LOCALAPPDATA")

    val gitnuroDir = File(localAppData).openDirectory("Gitnuro")
    val logsDir = gitnuroDir.openDirectory("logs")
    return "${logsDir.absolutePath}/gitnuro.log"
}

private fun linuxLogsPathAppender(): String {
    // Based on this https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html
    val homePath = System.getProperty("user.home")
    val xdgStateHome = System.getenv("XDG_STATE_HOME")

    val safeXdgStateHome = if (xdgStateHome.isNullOrBlank())
        "$homePath/.local/state"
    else
        xdgStateHome

    val gitnuroDir = File(safeXdgStateHome).openDirectory("gitnuro")
    val logsDir = gitnuroDir.openDirectory("logs")

    return "$logsDir/gitnuro.log"
}
