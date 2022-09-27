package com.jetpackduba.gitnuro.git.workspace

import javax.inject.Inject

class GetLinesFromTextUseCase @Inject constructor() {
    operator fun invoke(content: String, lineDelimiter: String?): List<String> {
        var splitted: List<String> = if (lineDelimiter != null) {
            content.split(lineDelimiter).toMutableList().apply {
                if (this.last() == "")
                    removeLast()
            }
        } else {
            listOf(content)
        }

        splitted = splitted.mapIndexed { index, line ->
            val lineWithBreak = line + lineDelimiter.orEmpty()

            if (index == splitted.count() - 1 && !content.endsWith(lineWithBreak)) {
                line
            } else
                lineWithBreak
        }

        return splitted
    }
}