package app.extensions

import java.awt.Desktop
import java.net.URI
import java.nio.file.FileSystems

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