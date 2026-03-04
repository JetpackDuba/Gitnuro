package com.jetpackduba.gitnuro.system

import com.jetpackduba.gitnuro.common.printError
import java.awt.Desktop
import java.io.File
import javax.inject.Inject

private const val TAG = "SystemUtils"

/**
 * Opens a file with the default external app.
 * An example would be opening an image with the default image viewer
 */
class OpenFileInExternalAppGitAction @Inject constructor(
    private val openPathInSystemGitAction: OpenPathInSystemGitAction,
) {
    operator fun invoke(filePath: String) {
        if (!openPathInSystemGitAction(filePath)) {
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
