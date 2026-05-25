package com.jetpackduba.gitnuro.repositoryopen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.collectLatestInCoroutineScope
import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.extensions.filePath
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.models.DiffSelected
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.GetCommitDiffEntriesUseCase
import com.jetpackduba.gitnuro.extensions.stateIn
import com.jetpackduba.gitnuro.ui.tree_files.entriesToTreeEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommitChangesViewModelExtender @AssistedInject constructor(
    private val getCommitDiffEntriesUseCase: GetCommitDiffEntriesUseCase,
    private val tabState: TabInstanceRepository,
    @Assisted private val viewModelScope: CoroutineScope,
    @Assisted private val showAsTree: Flow<Boolean>,
    @Assisted private val selectedItem: StateFlow<SelectedItem>,
    @Assisted private val diffSelected: StateFlow<DiffSelected?>,
    @Assisted private val onDiffSelected: (DiffSelected) -> Unit,
    @Assisted private val onAlternateShowAsTree: () -> Unit,
) : CoroutineScope by viewModelScope {

    @AssistedFactory
    interface Factory {
        fun create(
            viewModelScope: CoroutineScope,
            showAsTree: Flow<Boolean>,
            selectedItem: StateFlow<SelectedItem>,
            diffSelected: StateFlow<DiffSelected?>,
            onDiffSelected: (DiffSelected) -> Unit,
            onAlternateShowAsTree: () -> Unit,
        ): CommitChangesViewModelExtender
    }

    val showSearch: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val searchFilter: StateFlow<TextFieldValue>
        field = MutableStateFlow(TextFieldValue(""))

    val changesLazyListState = MutableStateFlow(
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    )

    val textScroll = MutableStateFlow(ScrollState(0))

    private val commitChangesTreeContractedDirectories = MutableStateFlow(emptyList<String>())


    val commitChangesState = combine(
        selectedItem,
        showAsTree,
        commitChangesTreeContractedDirectories,
    ) { item, showAsTree, treeContractedDirectories ->
        flow<CommitChangesStateUi?> {
            emit(CommitChangesStateUi.Loading)
// TODO
//            showSearch.value = false
//            searchFilter.value = TextFieldValue("")
//            changesLazyListState.value = LazyListState(
//                0,
//                0
//            )
//            textScroll.value = ScrollState(0)

            if (item is SelectedItem.Commit) {
                val changes = getCommitDiffEntriesUseCase(item.commit)

                val state = when (changes) {
                    is Either.Err -> {
                        CommitChangesStateUi.Error(changes.error)
                    }

                    is Either.Ok -> {
                        CommitChangesStateUi.Loaded(
                            commit = item.commit,
                            changes = changes.value,
                            changesTree = entriesToTreeEntry(
                                showAsTree = showAsTree, changes.value, treeContractedDirectories
                            ) { it.filePath },
                            showAsTree = showAsTree,
                            showSearch = false,
                            searchFilter = TextFieldValue(""),
                            treeContractedDirectories = treeContractedDirectories,
                        )
                    }
                }

                emit(state)
            } else {
                emit(null)
            }
        }
    }.flattenConcat().combine(showSearch, searchFilter) { state, showSearch, searchFilter ->
            when (state) {
                is CommitChangesStateUi.Loaded -> {
                    val changesFiltered = if (showSearch && searchFilter.text.isNotBlank()) {
                        state.changes.filter { it.filePath.lowercaseContains(searchFilter.text) }
                    } else {
                        emptyList()
                    }

                    state.copy(
                        showSearch = showSearch,
                        changesFiltered = changesFiltered,
                        changesTreeFiltered = entriesToTreeEntry(
                            showAsTree = state.showAsTree,
                            changesFiltered,
                            state.treeContractedDirectories,
                        ) { it.filePath },
                    )
                }

                else -> state
            }
        }
        .stateIn(CommitChangesStateUi.Loading)

    init {
        showSearch.collectLatestInCoroutineScope {
            if (it) {
                addSearchToCloseableView()
            } else {
                removeCommitChangesSearchFromCloseableView()
            }
        }
    }

    fun onAction(action: CommitChangesAction) {
        when (action) {
            is CommitChangesAction.SearchFilterChanged -> searchFilter.value = action.filter
            is CommitChangesAction.SearchFilterToggle -> showSearch.value = action.show
            is CommitChangesAction.SelectEntry -> {
                onDiffSelected(DiffSelected.CommitedChanges(setOf(DiffType.CommitDiff(action.entry))))
            }

            CommitChangesAction.ToggleShowAsTree -> onAlternateShowAsTree()

            is CommitChangesAction.TreeDirectoryToggle -> onDirectoryVisibilityToggle(action.path)
            CommitChangesAction.AddSearchToCloseables -> addSearchToCloseableView()
        }
    }

    private fun addSearchToCloseableView() = viewModelScope.launch {
        tabState.addCloseableView(CloseableView.COMMIT_CHANGES_SEARCH)
    }


    private fun removeCommitChangesSearchFromCloseableView() = viewModelScope.launch {
        tabState.removeCloseableView(CloseableView.COMMIT_CHANGES_SEARCH)
    }

    fun onDirectoryVisibilityToggle(directoryPath: String) {
        val contractedDirectories = commitChangesTreeContractedDirectories.value

        if (contractedDirectories.contains(directoryPath)) {
            commitChangesTreeContractedDirectories.value -= directoryPath
        } else {
            commitChangesTreeContractedDirectories.value += directoryPath
        }
    }

    fun searchFilterToggled(show: Boolean) {
        showSearch.value = show
        searchFilter.value = TextFieldValue("")
    }

}