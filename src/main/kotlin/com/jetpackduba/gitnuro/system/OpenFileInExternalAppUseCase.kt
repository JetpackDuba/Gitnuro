package com.jetpackduba.gitnuro.system

import com.jetpackduba.gitnuro.logging.printError
import java.awt.Desktop
import java.io.File
import javax.inject.Inject

private const val TAG = "SystemUtils"

/**
 * Opens a file with the default external app.
 * An example would be opening an image with the default image viewer
 */
class OpenFileInExternalAppUseCase @Inject constructor(
    private val openPathInSystemUseCase: OpenPathInSystemUseCase
) {
    operator fun invoke(filePath: String) {
        if (!openPathInSystemUseCase(filePath)) {
            openFileJdk(filePath)
        }
    }

    private fun openFileJdk(filePath: String) {
        try {
            Desktop.getDesktop().open(File(filePath))
        } catch (ex: Exception) {
            printError(TAG, "Failed to open URL in browser")
            ex.printStackTrace()
        }
    }
}
