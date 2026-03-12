package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.diff.RawText

interface IGetLinesFromRawTextGitAction {
    operator fun invoke(rawFile: RawText): List<String>
}