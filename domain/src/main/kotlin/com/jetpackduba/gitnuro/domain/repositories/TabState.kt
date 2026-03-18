package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.exceptions.GitnuroException
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.GraphCommit
import com.jetpackduba.gitnuro.domain.models.Notification
import com.jetpackduba.gitnuro.domain.models.ProcessingState
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.newErrorNow
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

private const val TAG = "TabState"

@TabScope
class TabInstanceRepository @Inject constructor(
    val errorsRepository: IErrorsRepository,
    private val scope: CoroutineScope,
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

    private val _processing = MutableStateFlow<ProcessingState>(ProcessingState.None)
    val processing: StateFlow<ProcessingState> = _processing

    fun initGit(git: Git) {
        this.unsafeGit = git
    }

    @Synchronized
    fun safeProcessing(
        refreshType: RefreshType,
        // TODO Eventually the title and subtitles should be mandatory but for now the default it's empty to slowly
        //  migrate the code that uses this function
        title: String = "",
        subtitle: String = "",
        taskType: TaskType,
        refreshEvenIfCrashes: Boolean = false,
        refreshEvenIfCrashesInteractive: ((Exception) -> Boolean)? = null,
        callback: suspend (git: Git) -> Notification?,
    ): Job {
        val job = scope.launch(Dispatchers.IO) {
            var hasProcessFailed = false
            var refreshEvenIfCrashesInteractiveResult = false
            operationRunning = true


            try {
                _processing.update { processingState ->
                    if (processingState is ProcessingState.None) {
                        ProcessingState.Processing(title, subtitle)
                    } else {
                        processingState
                    }
                }
                val notification = callback(git)

                if (notification != null) {
                    errorsRepository.emitNotification(notification)
                }
            } catch (ex: Exception) {
                hasProcessFailed = true
                ex.printStackTrace()

                refreshEvenIfCrashesInteractiveResult = refreshEvenIfCrashesInteractive?.invoke(ex) ?: false

                val containsCancellation = exceptionContainsCancellation(ex)

                if (!containsCancellation) {
                    val innerException = getInnerException(ex)

                    errorsRepository.addError(newErrorNow(taskType, innerException))
                }

                printError(TAG, ex.message.orEmpty(), ex)
            } finally {
                _processing.value = ProcessingState.None
                operationRunning = false
                lastOperation = System.currentTimeMillis()

                if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes || refreshEvenIfCrashesInteractiveResult)) {
                    refreshData.emit(refreshType)
                }
            }
        }

        this.currentJob = job

        return job
    }

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

    fun safeProcessingWithoutGit(
        // TODO Eventually the title and subtitles should be mandatory but for now the default it's empty to slowly
        //  migrate the code that uses this function
        title: String = "",
        subtitle: String = "",
        callback: suspend CoroutineScope.() -> Unit,
    ): Job {
        val job = scope.launch(Dispatchers.IO) {
            _processing.value = ProcessingState.Processing(title, subtitle)
            operationRunning = true

            try {
                this.callback()
            } catch (ex: Exception) {
                ex.printStackTrace()

                val containsCancellation = exceptionContainsCancellation(ex)

                if (!containsCancellation)
                    errorsRepository.addError(
                        newErrorNow(
                            taskType = TaskType.UNSPECIFIED, ex
                        )
                    )

                printError(TAG, ex.message.orEmpty(), ex)
            } finally {
                _processing.value = ProcessingState.None
                operationRunning = false
            }
        }

        this.currentJob = job

        return job
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

            if (showError)
                errorsRepository.addError(
                    newErrorNow(
                        taskType = TaskType.UNSPECIFIED, ex
                    )
                )

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

    fun newSelectedCommit(revCommit: GraphCommit?) = runOperation(
        refreshType = RefreshType.NONE,
    ) { _ ->
        if (revCommit == null) {
            newSelectedItem(SelectedItem.None)
        } else {
            val newSelectedItem = SelectedItem.Commit(revCommit.commit)
            newSelectedItem(newSelectedItem)
        }
    }

    suspend fun newSelectedItem(selectedItem: SelectedItem) {
        _selectedItem.value = selectedItem
    }

    fun newSelectedRef(ref: Branch, hash: String) = runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        //if (objectId == null) {
        newSelectedItem(SelectedItem.None)
        /*} else {
            val commit = findCommitGitAction(git, objectId)

            if (commit == null) {
                newSelectedItem(SelectedItem.None)
            } else {
                val newSelectedItem = SelectedItem.Ref(ref, commit)
                newSelectedItem(newSelectedItem)
            }
        }*/
    }

    fun newSelectedRef(ref: Ref, hash: ObjectId) = runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        //if (objectId == null) {
        newSelectedItem(SelectedItem.None)
        /*} else {
            val commit = findCommitGitAction(git, objectId)

            if (commit == null) {
                newSelectedItem(SelectedItem.None)
            } else {
                val newSelectedItem = SelectedItem.Ref(ref, commit)
                newSelectedItem(newSelectedItem)
            }
        }*/
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun refreshFlowFiltered(vararg filters: RefreshType, callback: suspend (RefreshType) -> Unit) {
        refreshData
            .filter { refreshType ->
                filters.contains(refreshType)
            }.collect {
                try {
                    callback(it)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    errorsRepository.addError(
                        newErrorNow(
                            taskType = TaskType.UNSPECIFIED, ex
                        )
                    )
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