package com.jetpackduba.gitnuro.system

import com.jetpackduba.gitnuro.logging.printError
import java.awt.Desktop
import java.net.URI
import javax.inject.Inject

private const val TAG = "SystemUtils"

/**
 * Opens a URL in the default system browser
 */
class OpenUrlInBrowserUseCase @Inject constructor(
    private val openPathInSystemUseCase: OpenPathInSystemUseCase,
) {
    operator fun invoke(url: String) {
        if (!openPathInSystemUseCase(url)) {
            openUrlInBrowserJdk(url)
        }
    }


    private fun openUrlInBrowserJdk(url: String) {
        try {
            Desktop.getDesktop().browse(URI(url))
        } catch (ex: Exception) {
            printError(TAG, "Failed to open URL in browser")
            ex.printStackTrace()
        }
    }
}
