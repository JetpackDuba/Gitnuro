package com.jetpackduba.gitnuro.domain.git.diff

import com.jetpackduba.gitnuro.domain.git.EntryContent
import org.eclipse.jgit.patch.FileHeader

data class DiffContent(
    val fileHeader: FileHeader,
    val rawOld: EntryContent,
    val rawNew: EntryContent,
)
