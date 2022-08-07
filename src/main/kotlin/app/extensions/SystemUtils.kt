package app.extensions

import app.logging.printLog
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
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

private fun openSystemSpecific(url: String): Boolean {
    val os = System.getProperty("os.name")
    if (os.contains("linux")) {
        if (runCommandWithoutResult("xdg-open", "%s", url))
            return true
        if (runCommandWithoutResult("kde-open", "%s", url))
            return true
        if (runCommandWithoutResult("gnome-open", "%s", url))
            return true
    } else if (os.contains("windows")) {
        if (runCommandWithoutResult("explorer", "%s", url))
            return true
    } else if (os.contains("mac")) {
        if (runCommandWithoutResult("open", "%s", url))
            return true
    }

    return false
}

fun openUrlInBrowserJdk(url: String) {

    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (ex: Exception) {
        println("Failed to open URL in browser")
        ex.printStackTrace()
    }
}

fun copyInBrowser(textToCopy: String) {
    try {
        val selection = StringSelection(textToCopy)
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    } catch (ex: Exception) {
        printLog(TAG, "Failed to copy text")
        ex.printStackTrace()
    }
}