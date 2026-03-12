package com.jetpackduba.gitnuro.domain.models

import org.eclipse.jgit.patch.FileHeader

data class DiffContent(
    val fileHeader: FileHeader,
    val rawOld: EntryContent,
    val rawNew: EntryContent,
)