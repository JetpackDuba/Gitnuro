package app.viewmodels

import app.git.DiffEntryType
import app.git.diff.DiffResult

sealed interface ViewDiffResult {
    object None : ViewDiffResult
    object Loading : ViewDiffResult
    object DiffNotFound : ViewDiffResult
    data class Loaded(val diffEntryType: DiffEntryType, val diffResult: DiffResult) : ViewDiffResult
}