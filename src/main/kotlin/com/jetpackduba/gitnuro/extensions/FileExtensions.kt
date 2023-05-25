package com.jetpackduba.gitnuro.extensions

import java.io.File

fun File.openDirectory(dirName: String): File {
    val newDir = File(this, dirName)

    if (!newDir.exists() || !newDir.isDirectory) {
        newDir.mkdirs()
    }

    return newDir
}