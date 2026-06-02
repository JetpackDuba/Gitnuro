package com.jetpackduba.gitnuro.ui.status

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import com.jetpackduba.gitnuro.ui.tree_files.entriesToTreeEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.eclipse.jgit.lib.RepositoryState

sealed interface SelectionType<T> {
    data class SetSingleEntry<T>(val entry: T) : SelectionType<T>
    data class AppendSingleEntry<T>(val entry: T) : SelectionType<T>
    data class RemoveSingleEntry<T>(val entry: T) : SelectionType<T>
    data class AddMultipleEntries<T>(val entries: List<T>) : SelectionType<T>
}


@Stable
data class StatusState(
    val isLoading: Boolean = true,
    val staged: List<TreeItem<StatusEntry>> = emptyList(),
    val filteredStaged: List<TreeItem<StatusEntry>> = emptyList(),
    val unstaged: List<TreeItem<StatusEntry>> = emptyList(),
    val filteredUnstaged: List<TreeItem<StatusEntry>> = emptyList(),
    val swapUncommittedChanges: Boolean = false,
    val isAmend: Boolean = false,
    val isAmendRebaseInteractive: Boolean = false,
    val committerDataRequestState: CommitterDataRequestState = CommitterDataRequestState.None,
    val rebaseInteractiveState: RebaseInteractiveState = RebaseInteractiveState.None,
    val selectedUnstagedDiffEntries: List<DiffType.UncommittedDiff> = emptyList(),
    val selectedStagedDiffEntries: List<DiffType.UncommittedDiff> = emptyList(),
    val showSearchStaged: Boolean = false,
    val searchFilterStaged: TextFieldValue = TextFieldValue(""),
    val showSearchUnstaged: Boolean = false,
    val searchFilterUnstaged: TextFieldValue = TextFieldValue(""),
    val showAsTree: Boolean = false,
    val previousCommitMessage: String? = null,
    val repositoryState: RepositoryState = RepositoryState.SAFE,
) {
    val hasPreviousCommits: Boolean = previousCommitMessage != null

    val haveConflictsBeenSolved: Boolean = unstaged.none {
        it is TreeItem.File && it.data.statusType == StatusType.CONFLICTING
    }

    fun getEntriesByEntryType(entryType: EntryType): List<StatusEntry> {
        return when (entryType) {
            EntryType.STAGED -> staged.mapNotNull { (it as? TreeItem.File)?.data }
            EntryType.UNSTAGED -> unstaged.mapNotNull { (it as? TreeItem.File)?.data }
        }
    }

    val hasStagedFiles = staged.isNotEmpty()
    val hasUnstagedFiles = unstaged.isNotEmpty()
}

fun combineStatusState(
    status: Flow<Status>,
    showSearchStaged: MutableStateFlow<Boolean>,
    searchFilterStaged: MutableStateFlow<TextFieldValue>,
    showSearchUnstaged: MutableStateFlow<Boolean>,
    searchFilterUnstaged: MutableStateFlow<TextFieldValue>,
    showAsTree: Flow<Boolean>,
    treeContractedDirectories: MutableStateFlow<List<String>>,
    swapUncommittedChanges: Flow<Boolean>,
    isAmend: Flow<Boolean>,
    isAmendRebaseInteractive: Flow<Boolean>,
    committerDataRequestState: Flow<CommitterDataRequestState>,
    rebaseInteractiveState: Flow<RebaseInteractiveState>,
    selectedUnstagedDiffEntries: Flow<List<DiffType.UncommittedDiff>>,
    selectedStagedDiffEntries: Flow<List<DiffType.UncommittedDiff>>,
    previousCommitMessage: Flow<String?>,
    repositoryState: Flow<RepositoryState>,
): Flow<StatusState> {
    return combine(
        status,
        showSearchStaged,
        searchFilterStaged,
        showSearchUnstaged,
        searchFilterUnstaged,
        showAsTree,
        treeContractedDirectories,
        swapUncommittedChanges,
        isAmend,
        isAmendRebaseInteractive,
        committerDataRequestState,
        rebaseInteractiveState,
        selectedUnstagedDiffEntries,
        selectedStagedDiffEntries,
        previousCommitMessage,
        repositoryState,
    ) {
            status,
            showSearchStaged,
            searchFilterStaged,
            showSearchUnstaged,
            searchFilterUnstaged,
            showAsTree,
            contractedDirectories,
            swapUncommittedChanges,
            isAmend,
            isAmendRebaseInteractive,
            committerDataRequestState,
            rebaseInteractiveState,
            selectedUnstagedDiffEntries,
            selectedStagedDiffEntries,
            previousCommitMessage,
            repositoryState,
        ->
        val filteredUnstaged = if (showSearchUnstaged && searchFilterUnstaged.text.isNotBlank()) {
            status.unstaged.filter { it.filePath.lowercaseContains(searchFilterUnstaged.text) }
        } else {
            status.unstaged
        }.prioritizeConflicts()

        val filteredStaged = if (showSearchStaged && searchFilterStaged.text.isNotBlank()) {
            status.staged.filter { it.filePath.lowercaseContains(searchFilterStaged.text) }
        } else {
            status.staged
        }.prioritizeConflicts()
        StatusState(
            isLoading = false,
            staged = statusEntriesToTreeEntry(
                showAsTree,
                status.staged,
                contractedDirectories
            ),
            filteredStaged = statusEntriesToTreeEntry(
                showAsTree,
                filteredStaged,
                contractedDirectories
            ),
            unstaged = statusEntriesToTreeEntry(
                showAsTree,
                status.unstaged,
                contractedDirectories
            ),
            filteredUnstaged = statusEntriesToTreeEntry(
                showAsTree,
                filteredUnstaged,
                contractedDirectories
            ),
            swapUncommittedChanges = swapUncommittedChanges,
            isAmend = isAmend,
            isAmendRebaseInteractive = isAmendRebaseInteractive,
            committerDataRequestState = committerDataRequestState,
            rebaseInteractiveState = rebaseInteractiveState,
            selectedUnstagedDiffEntries = selectedUnstagedDiffEntries,
            selectedStagedDiffEntries = selectedStagedDiffEntries,
            showSearchStaged = showSearchStaged,
            searchFilterStaged = searchFilterStaged,
            showSearchUnstaged = showSearchUnstaged,
            searchFilterUnstaged = searchFilterUnstaged,
            showAsTree = showAsTree,
            previousCommitMessage = previousCommitMessage,
            repositoryState = repositoryState,
        )
    }
}

private fun statusEntriesToTreeEntry(
    showAsTree: Boolean,
    entries: List<StatusEntry>,
    contractedDirectories: List<String>
): List<TreeItem<StatusEntry>> {
    return entriesToTreeEntry(
        showAsTree,
        entries,
        contractedDirectories
    ) { it.filePath }
}

private fun List<StatusEntry>.prioritizeConflicts(): List<StatusEntry> {
    return this.groupBy { it.filePath }
        .map {
            val statusEntries = it.value
            return@map if (statusEntries.count() == 1) {
                statusEntries.first()
            } else {
                val conflictingEntry =
                    statusEntries.firstOrNull { entry -> entry.statusType == StatusType.CONFLICTING }

                conflictingEntry ?: statusEntries.first()
            }
        }
}


data class CommitMessage(val message: String, val type: MessageType)

enum class MessageType {
    NORMAL,
    MERGE;
}

sealed interface CommitterDataRequestState {
    data object None : CommitterDataRequestState
    data class WaitingInput(val authorInfo: AuthorInfo) : CommitterDataRequestState
    data class Accepted(val authorInfo: AuthorInfo, val persist: Boolean) : CommitterDataRequestState
    object Reject : CommitterDataRequestState
}
