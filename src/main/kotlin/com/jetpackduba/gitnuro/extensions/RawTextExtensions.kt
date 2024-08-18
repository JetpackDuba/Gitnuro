package com.jetpackduba.gitnuro.extensions

import org.eclipse.jgit.diff.RawText
import java.io.ByteArrayOutputStream

fun RawText.lineAt(line: Int): String {
    return this.getString(line, line + 1, false)
}