package app.viewmodels

import app.git.DiffEntryType
import app.git.diff.DiffResult

sealed interface ViewDiffResult {
    object None : ViewDiffResult

    data class Loading(val filePath: String) : ViewDiffResult

    object DiffNotFound : ViewDiffResult

    data class Loaded(val diffEntryType: DiffEntryType, val diffResult: DiffResult) : ViewDiffResult
}