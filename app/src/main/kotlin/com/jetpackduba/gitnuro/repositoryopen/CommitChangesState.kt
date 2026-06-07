package com.jetpackduba.gitnuro.repositoryopen

import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import org.eclipse.jgit.diff.DiffEntry

data class CommitChangesState(
    val isLoading: Boolean,
    val error: AppError? = null,
    val commit: Commit,
    val showAsTree: Boolean,
    val showSearch: Boolean,
    val searchFilter: TextFieldValue,
    val treeContractedDirectories: List<String> = emptyList(),
    val changes: List<DiffEntry> = emptyList(),
    val changesFiltered: List<DiffEntry> = emptyList(),
    val changesTree: List<TreeItem<DiffEntry>> = emptyList(),
    val changesTreeFiltered: List<TreeItem<DiffEntry>> = emptyList(),
)
