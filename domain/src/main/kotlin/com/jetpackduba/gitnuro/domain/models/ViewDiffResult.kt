package com.jetpackduba.gitnuro.domain.models

sealed interface ViewDiffResult {
    object None : ViewDiffResult

    data class Loading(val diffType: DiffType) : ViewDiffResult

    data class DiffNotFound(val diff: DiffType?) : ViewDiffResult

    data class Loaded(val diffType: DiffType, val diffResult: DiffResult) : ViewDiffResult
}