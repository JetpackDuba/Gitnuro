package app.extensions

import java.awt.Desktop
import java.net.URI
import java.nio.file.FileSystems

val systemSeparator: String by lazy {
    FileSystems.getDefault().separator
}

fun openUrlInBrowser(url: String) {
    Desktop.getDesktop().browse(URI(url))
}