package app

import app.di.TabScope
import javax.inject.Inject
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists

@TabScope
class TempFilesManager @Inject constructor() {
    val tempDir by lazy {
        val tempDirPath = createTempDirectory("gitnuro_")
        tempDirPath.toFile().deleteOnExit()

        tempDirPath
    }

    fun removeTempDir() {
        tempDir.deleteIfExists()
    }
}