package com.jetpackduba.gitnuro.domain.models

sealed interface ViewDiffResult {
    val diffType: DiffType?

    object None : ViewDiffResult {
        override val diffType: DiffType? = null
    }

    data class Loading(override val diffType: DiffType) : ViewDiffResult

    data class DiffNotFound(override val diffType: DiffType?) : ViewDiffResult

    data class Loaded(override val diffType: DiffType, val diffResult: DiffResult) : ViewDiffResult
}