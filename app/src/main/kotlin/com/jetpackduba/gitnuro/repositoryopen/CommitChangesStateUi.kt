package com.jetpackduba.gitnuro.repositoryopen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.extensions.filePath
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import com.jetpackduba.gitnuro.ui.tree_files.entriesToTreeEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.eclipse.jgit.diff.DiffEntry
import org.jetbrains.skiko.currentSystemTheme

sealed interface CommitChangesStateUi {
    data object Loading : CommitChangesStateUi
    data class Error(val error: AppError) : CommitChangesStateUi

    data class Loaded(
        val commit: Commit,
        val showAsTree: Boolean,
        val showSearch: Boolean,
        val searchFilter: TextFieldValue,
        val treeContractedDirectories: List<String>,
        val changes: List<DiffEntry>,
        val changesFiltered: List<DiffEntry> = emptyList(),
        val changesTree: List<TreeItem<DiffEntry>>,
        val changesTreeFiltered: List<TreeItem<DiffEntry>> = emptyList(),
    ): CommitChangesStateUi
}
