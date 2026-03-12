package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Line

interface ITextDiffFromDiffLinesGitAction {
    operator fun invoke(lines: List<Line>): List<Line>
}