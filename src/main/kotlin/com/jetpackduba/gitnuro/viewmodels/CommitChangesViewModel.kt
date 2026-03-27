package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.CloseableView
import com.jetpackduba.gitnuro.git.DiffType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.diff.GetCommitDiffEntriesUseCase
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.repositories.DiffSelected
import com.jetpackduba.gitnuro.repositories.SelectedDiffItemRepository
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.ui.selectedCommits
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import com.jetpackduba.gitnuro.ui.tree_files.entriesToTreeEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File
import javax.inject.Inject

private const val MIN_TIME_IN_MS_TO_SHOW_LOAD = 300L

class CommitChangesViewModel @Inject constructor(
    private val tabState: TabState,
    private val getCommitDiffEntriesUseCase: GetCommitDiffEntriesUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val tabScope: CoroutineScope,
    private val selectedDiffItemRepository: SelectedDiffItemRepository,
) {
    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch

    private val _searchFilter = MutableStateFlow(TextFieldValue(""))
    val searchFilter: StateFlow<TextFieldValue> = _searchFilter

    val changesLazyListState = MutableStateFlow(
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    )

    val textScroll = MutableStateFlow(ScrollState(0))

    val showAsTree = appSettingsRepository.showChangesAsTreeFlow

    val diffSelected = selectedDiffItemRepository.diffSelected.map { it as? DiffSelected.CommitedChanges }

    private val treeContractedDirectories = MutableStateFlow(emptyList<String>())

    private val _commitChangesState = MutableStateFlow<CommitChangesState>(CommitChangesState.Loading)

    private val commitChangesFiltered =
        combine(_commitChangesState, _showSearch, _searchFilter) { state, showSearch, filter ->
            if (state is CommitChangesState.Loaded && showSearch && filter.text.isNotBlank()) {
                state.copy(changes = state.changes.filter { it.filePath.lowercaseContains(filter.text) })
            } else {
                state
            }
        }

    val commitChangesStateUi: StateFlow<CommitChangesStateUi> = combine(
        commitChangesFiltered,
        showAsTree,
        treeContractedDirectories,
    ) { commitState, showAsTree, contractedDirs ->
        when (commitState) {
            CommitChangesState.Loading -> CommitChangesStateUi.Loading
            is CommitChangesState.Loaded -> {
                if (showAsTree) {
                    CommitChangesStateUi.TreeLoaded(
                        selection = commitState.selection,
                        changes = entriesToTreeEntry(showAsTree, commitState.changes, contractedDirs) { it.filePath }
                    )
                } else {
                    CommitChangesStateUi.ListLoaded(
                        selection = commitState.selection,
                        changes = commitState.changes
                    )
                }
            }
        }
    }
        .stateIn(
            tabScope,
            SharingStarted.Lazily,
            CommitChangesStateUi.Loading
        )

    init {
        tabScope.launch {
            _showSearch.collectLatest {
                if (it) {
                    addSearchToCloseableView()
                } else {
                    removeSearchFromCloseableView()
                }
            }
        }

        tabScope.launch {
            tabState.closeViewFlow.collectLatest {
                if (it == CloseableView.COMMIT_CHANGES_SEARCH) {
                    onSearchFilterToggled(false)
                }
            }
        }
    }

    fun loadChanges(selectedItem: SelectedItem.CommitBasedItem) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val selection = CommitChangesSelection.from(selectedItem)
        val state = _commitChangesState.value

        if (
            state is CommitChangesState.Loading ||
            state is CommitChangesState.Loaded && state.selection != selection
        ) {
            delayedStateChange(
                delayMs = MIN_TIME_IN_MS_TO_SHOW_LOAD,
                onDelayTriggered = { _commitChangesState.value = CommitChangesState.Loading }
            ) {
                val changes = loadSelectionChanges(git, selection)
                _commitChangesState.value = CommitChangesState.Loaded(selection, changes)
            }

            _showSearch.value = false
            _searchFilter.value = TextFieldValue("")
            changesLazyListState.value = LazyListState(0, 0)
            textScroll.value = ScrollState(0)
        }
    }

    private suspend fun loadSelectionChanges(
        git: org.eclipse.jgit.api.Git,
        selection: CommitChangesSelection,
    ): List<DiffEntry> {
        return if (selection.isMultiple) {
            val newestCommit = selection.newestCommit.fullData(git.repository) ?: return emptyList()
            val oldestCommit = selection.oldestCommit.fullData(git.repository) ?: return emptyList()
            val parentCommit = oldestCommit.parents.firstOrNull()?.fullData(git.repository)

            getCommitDiffEntriesUseCase.getDiffEntriesBetweenCommits(
                git = git,
                oldCommit = parentCommit,
                newCommit = newestCommit,
            )
        } else {
            loadSingleCommitChanges(git, selection.primaryCommit)
        }
    }

    private suspend fun loadSingleCommitChanges(
        git: org.eclipse.jgit.api.Git,
        commit: RevCommit,
    ): List<DiffEntry> {
        val fullCommit = commit.fullData(git.repository) ?: return emptyList()
        val changes = getCommitDiffEntriesUseCase(git, fullCommit).toMutableList()

        if (fullCommit.parentCount == 3) {
            val untrackedFilesCommit =
                fullCommit.parents?.firstOrNull {
                    val parentCommit = it.fullData(git.repository) ?: return@firstOrNull false

                    parentCommit.fullMessage.startsWith("untracked files on") && parentCommit.parentCount == 0
                }

            if (untrackedFilesCommit != null) {
                val untrackedFilesChanges = getCommitDiffEntriesUseCase(git, untrackedFilesCommit)

                if (untrackedFilesChanges.all { it.changeType == DiffEntry.ChangeType.ADD }) {
                    changes.addAll(untrackedFilesChanges)
                }
            }
        }

        return changes
    }

    fun alternateShowAsTree() {
        appSettingsRepository.showChangesAsTree = !appSettingsRepository.showChangesAsTree
    }

    fun openFileInFolder(folderPath: String?) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        if (folderPath != null) {
            val file = File(git.repository.workTree.absolutePath + File.separator + folderPath)
            file.openFileInFolder()
        }
    }

    fun onDirectoryClicked(directoryPath: String) {
        val contractedDirectories = treeContractedDirectories.value

        if (contractedDirectories.contains(directoryPath)) {
            treeContractedDirectories.value -= directoryPath
        } else {
            treeContractedDirectories.value += directoryPath
        }
    }

    fun onSearchFilterToggled(visible: Boolean) {
        _showSearch.value = visible
    }

    fun onSearchFilterChanged(filter: TextFieldValue) {
        _searchFilter.value = filter
    }

    fun addSearchToCloseableView() = tabScope.launch {
        tabState.addCloseableView(CloseableView.COMMIT_CHANGES_SEARCH)
    }

    fun selectEntries(entries: List<DiffEntry>) {
        val commitsCount = (_commitChangesState.value as? CommitChangesState.Loaded)?.selection?.commits?.count() ?: 1

        selectedDiffItemRepository.addDiffCommited(
            diffType = entries.map { DiffType.CommitDiff(it, commitsCount = commitsCount) },
            addToExisting = false,
        )
    }

    private fun removeSearchFromCloseableView() = tabScope.launch {
        tabState.removeCloseableView(CloseableView.COMMIT_CHANGES_SEARCH)
    }
}

private sealed interface CommitChangesState {
    data object Loading : CommitChangesState
    data class Loaded(val selection: CommitChangesSelection, val changes: List<DiffEntry>) : CommitChangesState
}

data class CommitChangesSelection(
    val commits: List<RevCommit>,
    val primaryCommit: RevCommit,
) {
    val isMultiple: Boolean
        get() = commits.count() > 1

    val newestCommit: RevCommit
        get() = commits.first()

    val oldestCommit: RevCommit
        get() = commits.last()

    companion object {
        fun from(selectedItem: SelectedItem.CommitBasedItem): CommitChangesSelection {
            val commits = selectedItem.selectedCommits.ifEmpty { listOf(selectedItem.revCommit) }
            return CommitChangesSelection(commits = commits, primaryCommit = selectedItem.revCommit)
        }
    }
}

sealed interface CommitChangesStateUi {
    data object Loading : CommitChangesStateUi

    sealed interface Loaded : CommitChangesStateUi {
        val selection: CommitChangesSelection
    }

    data class ListLoaded(override val selection: CommitChangesSelection, val changes: List<DiffEntry>) : Loaded

    data class TreeLoaded(override val selection: CommitChangesSelection, val changes: List<TreeItem<DiffEntry>>) : Loaded
}
