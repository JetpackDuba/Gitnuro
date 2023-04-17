package com.jetpackduba.gitnuro.system

import com.jetpackduba.gitnuro.logging.printLog
import java.nio.file.FileSystems

private const val TAG = "OS"

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


val systemSeparator: String by lazy {
    FileSystems.getDefault().separator
}
