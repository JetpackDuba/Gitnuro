package com.jetpackduba.gitnuro.git.workspace

import javax.inject.Inject

class GetLinesFromTextUseCase @Inject constructor() {
    operator fun invoke(content: String): List<String> {
        var splitted: List<String> = content.split("\n\n").toMutableList().apply {
            if (this.last() == "")
                removeLast()
        }

        splitted = splitted.mapIndexed { index, line ->
            val lineWithBreak = line + "\n\n"

            if (index == splitted.count() - 1 && !content.endsWith(lineWithBreak)) {
                line
            } else
                lineWithBreak
        }

        splitted = splitted.map {
            it.split("\n").toMutableList().apply {
                if (this.last() == "")
                    removeLast()
            }
        }.flatten()

        splitted = splitted.mapIndexed { index, line ->
            val lineWithBreak = line + "\n"

            if (index == splitted.count() - 1 && !content.endsWith(lineWithBreak)) {
                line
            } else
                lineWithBreak
        }

        return splitted
    }
}