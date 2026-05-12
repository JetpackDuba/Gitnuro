package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.exceptions.GitnuroException
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.jgit.api.Git
import javax.inject.Inject

private const val TAG = "TabState"

@TabScope
class TabInstanceRepository @Inject constructor(
    private val scope: TabCoroutineScope,
//    private val findCommitGitAction: FindCommitGitAction,
) {
    private val _selectedItem = MutableStateFlow<SelectedItem>(SelectedItem.UncommittedChanges)
    val selectedItem: StateFlow<SelectedItem> = _selectedItem

    var lastOperation: Long = 0
        private set

    private var unsafeGit: Git? = null
    val git: Git
        get() {
            val unsafeGit = this.unsafeGit
            if (unsafeGit == null) {
                throw CancellationException("Repository not available")
            } else
                return unsafeGit
        }

    private val refreshData = MutableSharedFlow<RefreshType>()
    private val closeableViews = ArrayDeque<CloseableView>()
    private val closeableViewsMutex = Mutex()

    private val _closeView = MutableSharedFlow<CloseableView>()
    val closeViewFlow = _closeView.asSharedFlow()

    /**
     * Property that indicates if a git operation is running
     */
    @set:Synchronized
    var operationRunning = false

    private var currentJob: Job? = null

    private fun getInnerException(ex: Exception): Exception {
        return if (ex is GitnuroException) {
            ex
        } else {
            val cause = ex.cause

            if (cause != null && cause is Exception) {
                getInnerException(cause)
            } else {
                ex
            }
        }
    }

    private fun exceptionContainsCancellation(ex: Throwable?): Boolean {
        return when (ex) {
            null -> false
            ex.cause -> false
            is CancellationException -> true
            else -> exceptionContainsCancellation(ex.cause)
        }
    }

    fun runOperation(
        showError: Boolean = false,
        refreshType: RefreshType,
        refreshEvenIfCrashes: Boolean = false,
        block: suspend (git: Git) -> Unit,
    ) = scope.launch(Dispatchers.IO) {
        var hasProcessFailed = false

        operationRunning = true

        try {
            block(git)
        } catch (ex: Exception) {
            ex.printStackTrace()

            hasProcessFailed = true

            printError(TAG, ex.message.orEmpty(), ex)
        } finally {
            if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes))
                refreshData.emit(refreshType)

            operationRunning = false
            lastOperation = System.currentTimeMillis()
        }
    }

    suspend fun refreshData(refreshType: RefreshType) {
        refreshData.emit(refreshType)
    }

    suspend fun newSelectedStash(stash: Commit) {
        newSelectedItem(SelectedItem.Stash(stash))
    }

    suspend fun noneSelected() {
        newSelectedItem(SelectedItem.None)
    }

    fun newSelectedCommit(revCommit: Commit?) {
        if (revCommit == null) {
            newSelectedItem(SelectedItem.None)
        } else {
            val newSelectedItem = SelectedItem.Commit(revCommit)
            newSelectedItem(newSelectedItem)
        }
    }

    fun newSelectedItem(selectedItem: SelectedItem) {
        _selectedItem.value = selectedItem
    }

    fun newSelectedRef(ref: Branch, hash: String) {}

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun refreshFlowFiltered(vararg filters: RefreshType, callback: suspend (RefreshType) -> Unit) {
        refreshData
            .filter { refreshType ->
                filters.contains(refreshType)
            }.collect {
                try {
                    callback(it)
                } catch (ex: Exception) {
                }
            }
    }

    suspend fun addCloseableView(view: CloseableView): Unit = closeableViewsMutex.withLock {
        closeableViews.remove(view) // Remove any previous elements if present
        closeableViews.add(view)
    }

    suspend fun removeCloseableView(view: CloseableView): Unit = closeableViewsMutex.withLock {
        closeableViews.remove(view)
    }

    suspend fun closeLastView(): Unit = closeableViewsMutex.withLock {
        val last = closeableViews.removeLastOrNull()

        if (last != null) {
            _closeView.emit(last)
        }
    }

    fun cancelCurrentTask() {
        currentJob?.cancel()
    }
}

enum class RefreshType {
    NONE,
    ALL_DATA,
    REPO_STATE,
    ONLY_LOG,
    STASHES,
    SUBMODULES,
    UNCOMMITTED_CHANGES,
    UNCOMMITTED_CHANGES_AND_LOG,
    REMOTES,
    REBASE_INTERACTIVE_STATE,
}

enum class CloseableView {
    DIFF,
    LOG_SEARCH,
    SIDE_PANE_SEARCH,
    COMMIT_CHANGES_SEARCH,
    STAGED_CHANGES_SEARCH,
    UNSTAGED_CHANGES_SEARCH,
}