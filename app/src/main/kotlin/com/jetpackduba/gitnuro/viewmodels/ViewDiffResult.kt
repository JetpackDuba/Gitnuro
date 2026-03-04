package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.git.DiffType
import com.jetpackduba.gitnuro.domain.git.diff.DiffResult

sealed interface ViewDiffResult {
    object None : ViewDiffResult

    data class Loading(val diffType: DiffType) : ViewDiffResult

    data class DiffNotFound(val diff: DiffType?) : ViewDiffResult

    data class Loaded(val diffType: DiffType, val diffResult: DiffResult) : ViewDiffResult
}