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