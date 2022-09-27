package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.diff.DiffResult

sealed interface ViewDiffResult {
    object None : ViewDiffResult

    data class Loading(val filePath: String) : ViewDiffResult

    object DiffNotFound : ViewDiffResult

    data class Loaded(val diffEntryType: DiffEntryType, val diffResult: DiffResult) : ViewDiffResult
}