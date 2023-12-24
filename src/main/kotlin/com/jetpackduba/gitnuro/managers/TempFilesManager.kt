package com.jetpackduba.gitnuro.managers

import com.jetpackduba.gitnuro.extensions.openDirectory
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