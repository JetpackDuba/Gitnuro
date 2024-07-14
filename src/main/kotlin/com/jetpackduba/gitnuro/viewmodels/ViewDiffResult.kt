package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.DiffType
import com.jetpackduba.gitnuro.git.diff.DiffResult

sealed interface ViewDiffResult {
    object None : ViewDiffResult

    data class Loading(val filePath: String) : ViewDiffResult

    object DiffNotFound : ViewDiffResult

    data class Loaded(val diffType: DiffType, val diffResult: DiffResult) : ViewDiffResult
}