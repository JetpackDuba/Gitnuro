package com.jetpackduba.gitnuro.domain.git.workspace

import org.eclipse.jgit.diff.RawText
import javax.inject.Inject

class GetLinesFromRawTextGitAction @Inject constructor(
    private val getLinesFromTextGitAction: GetLinesFromTextGitAction,
) {
    operator fun invoke(rawFile: RawText): List<String> {
        val content = rawFile.rawContent.toString(Charsets.UTF_8)

        return getLinesFromTextGitAction(content) // TODO Test this
    }
}