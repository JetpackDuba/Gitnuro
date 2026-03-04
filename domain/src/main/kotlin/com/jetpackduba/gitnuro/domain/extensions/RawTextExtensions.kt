package com.jetpackduba.gitnuro.domain.extensions

import org.eclipse.jgit.diff.RawText

fun RawText.lineAt(line: Int): String {
    return this.getString(line, line + 1, false)
}