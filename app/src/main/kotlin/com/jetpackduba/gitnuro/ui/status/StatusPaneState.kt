package com.jetpackduba.gitnuro.ui.status

import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.EntryType
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem

//data class StatusPaneState(
//
//)


sealed interface StatusPaneAction {
    data class EntryAction(val statusEntry: StatusEntry) : StatusPaneAction
    data class AllEntriesAction(val entryType: EntryType) : StatusPaneAction
    data class Reset(val statusEntry: StatusEntry) : StatusPaneAction
    data class Delete(val statusEntry: StatusEntry) : StatusPaneAction
    data class SelectEntry(
        val statusEntry: StatusEntry,
        val isCtrlPressed: Boolean,
        val isMetaPressed: Boolean,
        val isShiftPressed: Boolean,
        val diffEntries: List<TreeItem<StatusEntry>>,
        val selectedEntries: List<DiffType.UncommittedDiff>,
    ) : StatusPaneAction
    data class DiscardSelected(val entryType: EntryType) : StatusPaneAction
    data class SelectedEntriesAction(val entryType: EntryType) : StatusPaneAction
    data class CopyPath(val relative: Boolean, val entries: List<StatusEntry>) : StatusPaneAction
    data class OpenInFolder(val path: String) : StatusPaneAction
    data class TreeDirectoryToggle(val path: String) : StatusPaneAction
    data object ToggleShowAsTree : StatusPaneAction
    data class DirectoryAction(val path: String, val entryType: EntryType) : StatusPaneAction
    data class SearchFilterChanged(val filter: TextFieldValue, val entryType: EntryType) : StatusPaneAction
}