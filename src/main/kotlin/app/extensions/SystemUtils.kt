package app.extensions

import java.nio.file.FileSystems

val systemSeparator: String by lazy {
    FileSystems.getDefault().separator
}