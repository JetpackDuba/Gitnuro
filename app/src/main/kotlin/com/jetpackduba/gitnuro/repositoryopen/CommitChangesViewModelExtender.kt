package com.jetpackduba.gitnuro.repositoryopen

import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.collectLatestInCoroutineScope
import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.errOrNull
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.extensions.filePath
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.models.DiffSelected
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.usecases.GetCommitDiffEntriesUseCase
import com.jetpackduba.gitnuro.extensions.stateIn
import com.jetpackduba.gitnuro.ui.tree_files.entriesToTreeEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import kotlin.time.Duration.Companion.milliseconds

class CommitChangesViewModelExtender @AssistedInject constructor(
    private val getCommitDiffEntriesUseCase: GetCommitDiffEntriesUseCase,
    @Assisted private val viewModelScope: CoroutineScope,
    @Assisted private val showAsTree: StateFlow<Boolean>,
    @Assisted private val selectedItem: StateFlow<SelectedItem>,
    @Assisted private val diffSelected: StateFlow<DiffSelected?>,
    @Assisted private val onDiffSelected: (DiffSelected) -> Unit,
    @Assisted private val onAlternateShowAsTree: () -> Unit,
    @Assisted("addCloseableView") private val addCloseableView: (CloseableView) -> Unit,
    @Assisted("removeCloseableView") private val removeCloseableView: (CloseableView) -> Unit,
) : CoroutineScope by viewModelScope {

    @AssistedFactory
    interface Factory {
        fun create(
            viewModelScope: CoroutineScope,
            showAsTree: StateFlow<Boolean>,
            selectedItem: StateFlow<SelectedItem>,
            diffSelected: StateFlow<DiffSelected?>,
            onDiffSelected: (DiffSelected) -> Unit,
            onAlternateShowAsTree: () -> Unit,
            @Assisted("addCloseableView") addCloseableView: (CloseableView) -> Unit,
            @Assisted("removeCloseableView") removeCloseableView: (CloseableView) -> Unit,
        ): CommitChangesViewModelExtender
    }

    val showSearch: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val searchFilter: StateFlow<TextFieldValue>
        field = MutableStateFlow(TextFieldValue(""))

    private val commitChangesTreeContractedDirectories = MutableStateFlow(emptyList<String>())

    // Preserve the last loaded changes (from the previously selected commit) to prevent the UI from flickering while loading the data
    private val lastLoadedCommitsChanges = MutableStateFlow<List<DiffEntry>>(emptyList())

    val commitChangesState = combine(
        selectedItem,
        showAsTree,
        commitChangesTreeContractedDirectories,
    ) { item, showAsTree, treeContractedDirectories ->
        loadCommitChangesFlow(item, showAsTree, treeContractedDirectories)
    }
        .flattenConcat()
        .combine(showSearch, searchFilter) { state, showSearch, searchFilter ->
            val changesFiltered = if (showSearch && searchFilter.text.isNotBlank()) {
                state?.changes?.filter { it.filePath.lowercaseContains(searchFilter.text) }.orEmpty()
            } else {
                emptyList()
            }

            state?.copy(
                showSearch = showSearch,
                searchFilter = searchFilter,
                changesFiltered = changesFiltered,
                changesTreeFiltered = entriesToTreeEntry(
                    showAsTree = state.showAsTree,
                    changesFiltered,
                    state.treeContractedDirectories,
                ) { it.filePath },
            )
        }
        .onEach {
            if (it != null && !it.isLoading && it.changes.isNotEmpty()) {
                lastLoadedCommitsChanges.value = it.changes
            }
        }
        .stateIn(null as CommitChangesState?)

    private fun loadCommitChangesFlow(
        item: SelectedItem,
        showAsTree: Boolean,
        treeContractedDirectories: List<String>
    ) = channelFlow {
        if (item is SelectedItem.CommitItem) {
            send(
                CommitChangesState(
                    isLoading = false,
                    commit = item.commit,
                    showAsTree = showAsTree,
                    showSearch = false,
                    searchFilter = TextFieldValue(""),
                    changes = lastLoadedCommitsChanges.value
                )
            )

            val changesResultDeferred = async { getCommitDiffEntriesUseCase(item.commit) }

            val loadingJob = launch {
                delay(150.milliseconds)

                if (!changesResultDeferred.isCompleted) {
                    send(
                        CommitChangesState(
                            isLoading = true,
                            commit = item.commit,
                            showAsTree = showAsTree,
                            showSearch = false,
                            searchFilter = TextFieldValue(""),
                        )
                    )
                }
            }

            val changesResult: Either<List<DiffEntry>, AppError> = changesResultDeferred.await()
            loadingJob.cancel()

            val error = changesResult.errOrNull()
            val changes = changesResult.okOrNull()

            val state = CommitChangesState(
                commit = item.commit,
                changes = changes.orEmpty(),
                changesTree = entriesToTreeEntry(
                    showAsTree = showAsTree, changes.orEmpty(), treeContractedDirectories
                ) { it.filePath },
                showAsTree = showAsTree,
                showSearch = false,
                searchFilter = TextFieldValue(""),
                treeContractedDirectories = treeContractedDirectories,
                error = error,
                isLoading = false,
            )

            send(state)
        } else {
            send(null)
        }
    }


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
        addCloseableView(CloseableView.COMMIT_CHANGES_SEARCH)
    }


    private fun removeCommitChangesSearchFromCloseableView() = viewModelScope.launch {
        removeCloseableView(CloseableView.COMMIT_CHANGES_SEARCH)
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