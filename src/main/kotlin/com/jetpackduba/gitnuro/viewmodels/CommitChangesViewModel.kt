package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.extensions.fullData
import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.extensions.openFileInFolder
import com.jetpackduba.gitnuro.git.CloseableView
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.diff.GetCommitDiffEntriesUseCase
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
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
                        commit = commitState.commit,
                        changes = entriesToTreeEntry(commitState.changes, contractedDirs) { it.filePath }
                    )
                } else {
                    CommitChangesStateUi.ListLoaded(
                        commit = commitState.commit,
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

    fun loadChanges(commit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val state = _commitChangesState.value

        // Check if it's a different commit before resetting everything
        if (
            state is CommitChangesState.Loading ||
            state is CommitChangesState.Loaded && state.commit != commit
        ) {
            delayedStateChange(
                delayMs = MIN_TIME_IN_MS_TO_SHOW_LOAD,
                onDelayTriggered = { _commitChangesState.value = CommitChangesState.Loading }
            ) {
                val fullCommit = commit.fullData(git.repository)

                if (fullCommit != null) {
                    val changes = getCommitDiffEntriesUseCase(git, fullCommit).toMutableList()

                    if (fullCommit.parentCount == 3) {
                        val untrackedFilesCommit =
                            fullCommit.parents?.firstOrNull {
                                val parentCommit = it.fullData(git.repository) ?: return@firstOrNull false

                                parentCommit.fullMessage.startsWith("untracked files on") && parentCommit.parentCount == 0
                            }

                        if (untrackedFilesCommit != null) {
                            val untrackedFilesChanges = getCommitDiffEntriesUseCase(git, untrackedFilesCommit)

                            if (untrackedFilesChanges.all { it.changeType == DiffEntry.ChangeType.ADD }) { // All files should be new
                                changes.addAll(untrackedFilesChanges)
                            }
                        }
                    }

                    _commitChangesState.value = CommitChangesState.Loaded(commit, changes)
                }
            }

            _showSearch.value = false
            _searchFilter.value = TextFieldValue("")
            changesLazyListState.value = LazyListState(
                0,
                0
            )
            textScroll.value = ScrollState(0)
        }
    }

    fun alternateShowAsTree() {
        appSettingsRepository.showChangesAsTree = !appSettingsRepository.showChangesAsTree
    }

    fun openFileInFolder(folderPath: String?) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
    ) {
        folderPath?.let { File(it).openFileInFolder() }
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

    private fun removeSearchFromCloseableView() = tabScope.launch {
        tabState.removeCloseableView(CloseableView.COMMIT_CHANGES_SEARCH)
    }
}

private sealed interface CommitChangesState {
    data object Loading : CommitChangesState
    data class Loaded(val commit: RevCommit, val changes: List<DiffEntry>) :
        CommitChangesState
}

sealed interface CommitChangesStateUi {
    data object Loading : CommitChangesStateUi

    sealed interface Loaded : CommitChangesStateUi {
        val commit: RevCommit
    }

    data class ListLoaded(override val commit: RevCommit, val changes: List<DiffEntry>) :
        Loaded

    data class TreeLoaded(override val commit: RevCommit, val changes: List<TreeItem<DiffEntry>>) :
        Loaded
}

