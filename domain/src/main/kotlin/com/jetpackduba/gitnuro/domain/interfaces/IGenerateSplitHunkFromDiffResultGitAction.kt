package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.DiffResult
import com.jetpackduba.gitnuro.domain.models.SplitHunk

interface IGenerateSplitHunkFromDiffResultGitAction {
    operator fun invoke(diffFormat: DiffResult.Text): List<SplitHunk>
}