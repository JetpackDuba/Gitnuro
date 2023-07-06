package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.git.EntryContent
import org.eclipse.jgit.patch.FileHeader

data class DiffContent(
    val fileHeader: FileHeader,
    val rawOld: EntryContent,
    val rawNew: EntryContent,
)
