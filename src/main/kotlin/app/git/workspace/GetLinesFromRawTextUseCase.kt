package app.git.workspace

import org.eclipse.jgit.diff.RawText
import javax.inject.Inject

class GetLinesFromRawTextUseCase @Inject constructor(
    private val getLinesFromTextUseCase: GetLinesFromTextUseCase,
) {
    operator fun invoke(rawFile: RawText): List<String> {
        val content = rawFile.rawContent.toString(Charsets.UTF_8)//.removeSuffix(rawFile.lineDelimiter)
        val lineDelimiter: String? = rawFile.lineDelimiter

        return getLinesFromTextUseCase(content, lineDelimiter)
    }
}