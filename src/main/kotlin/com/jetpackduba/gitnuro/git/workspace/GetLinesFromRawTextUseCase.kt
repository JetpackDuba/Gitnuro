package com.jetpackduba.gitnuro.git.workspace

import org.eclipse.jgit.diff.RawText
import javax.inject.Inject

class GetLinesFromRawTextUseCase @Inject constructor(
    private val getLinesFromTextUseCase: GetLinesFromTextUseCase,
) {
    operator fun invoke(rawFile: RawText): List<String> {
        val content = rawFile.rawContent.toString(Charsets.UTF_8)

        return getLinesFromTextUseCase(content) // TODO Test this
    }
}