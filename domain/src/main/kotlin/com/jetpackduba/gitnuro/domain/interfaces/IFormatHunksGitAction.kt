package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Hunk
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.patch.FileHeader

interface IFormatHunksGitAction {
    operator fun invoke(
        fileHeader: FileHeader,
        rawOld: RawText,
        rawNew: RawText,
        isDisplayFullFile: Boolean,
    ): List<Hunk>
}