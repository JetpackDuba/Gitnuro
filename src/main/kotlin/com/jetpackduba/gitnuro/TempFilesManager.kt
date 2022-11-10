package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.extensions.openDirectory
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

@TabScope
class TempFilesManager @Inject constructor(
    private val appFilesManager: AppFilesManager,
) {
    fun tempDir(): File {
        val appDataDir = appFilesManager.getAppFolder()
        val tempDir = appDataDir.openDirectory("tmp")

        if(!tempDir.exists() || !tempDir.isDirectory) {
            tempDir.mkdir()
        }

        return tempDir
    }
}