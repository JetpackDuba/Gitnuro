package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.FileDiffType
import com.jetpackduba.gitnuro.git.diff.DiffResult

sealed interface ViewDiffResult {
    object None : ViewDiffResult

    data class Loading(val filePath: String) : ViewDiffResult

    object DiffNotFound : ViewDiffResult

    data class Loaded(val fileDiffType: FileDiffType, val diffResult: DiffResult) : ViewDiffResult
}