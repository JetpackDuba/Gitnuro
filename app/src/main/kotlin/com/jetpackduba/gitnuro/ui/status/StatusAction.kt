package com.jetpackduba.gitnuro.ui.status

import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.EntryType
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem

sealed interface StatusAction {
    data class EntryAction(val statusEntry: StatusEntry) : StatusAction
    data class AllEntriesAction(val entryType: EntryType) : StatusAction
    data class Reset(val statusEntry: StatusEntry) : StatusAction
    data class Delete(val statusEntry: StatusEntry) : StatusAction
    data class SelectEntry(
        val statusEntry: StatusEntry,
        val isCtrlPressed: Boolean,
        val isMetaPressed: Boolean,
        val isShiftPressed: Boolean,
        val diffEntries: List<TreeItem<StatusEntry>>,
        val selectedEntries: List<DiffType.UncommittedDiff>,
    ) : StatusAction
    data class DiscardSelected(val entryType: EntryType) : StatusAction
    data class SelectedEntriesAction(val entryType: EntryType) : StatusAction
    data class OpenInFolder(val path: String) : StatusAction
    data class TreeDirectoryToggle(val path: String) : StatusAction
    data object ToggleShowAsTree : StatusAction
    data class DirectoryAction(val path: String, val entryType: EntryType) : StatusAction
    data class SearchFilterChanged(val filter: TextFieldValue, val entryType: EntryType) : StatusAction
    data class Commit(val message: String) : StatusAction
    data class ContinueRebase(val message: String) : StatusAction
    data object SkipRebase : StatusAction
    data object AbortRebase : StatusAction
    data object ResetRepositoryState : StatusAction
    data class UpdateCommitMessage(val message: String) : StatusAction

    data object RejectCommitterData : StatusAction
    data class AcceptCommitterData(val authorInfo: AuthorInfo, val persist: Boolean) : StatusAction
    data class SearchFilterToggledStaged(val show: Boolean): StatusAction
    data class SearchFilterToggledUnstaged(val show: Boolean): StatusAction
    data object AddStagedSearchToCloseableView: StatusAction
    data object AddUnstagedSearchToCloseableView: StatusAction

    data class ToggleAmend(val toggle: Boolean): StatusAction
    data class ToggleAmendRebaseInteractive(val toggle: Boolean): StatusAction
}