package com.jetpackduba.gitnuro.extensions

import org.eclipse.jgit.diff.RawText
import java.io.ByteArrayOutputStream

fun RawText.lineAt(line: Int): String {
    val outputStream = ByteArrayOutputStream()
    this.writeLine(outputStream, line)
    return outputStream.toString(Charsets.UTF_8)
}