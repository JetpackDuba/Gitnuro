package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.git.log.FindCommitUseCase
import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.managers.ErrorsManager
import com.jetpackduba.gitnuro.managers.newErrorNow
import com.jetpackduba.gitnuro.ui.SelectedItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

private const val TAG = "TabState"

interface ProcessingInfo {
    fun changeSubtitle(newSubtitle: String)
    fun changeIsCancellable(newIsCancellable: Boolean)
}

sealed interface ProcessingState {
    object None : ProcessingState
    data class Processing(
        val title: String,
        val subtitle: String,
        val isCancellable: Boolean,
    ) : ProcessingState
}

@TabScope
class TabState @Inject constructor(
    val errorsManager: ErrorsManager,
    private val scope: CoroutineScope,
    private val findCommitUseCase: FindCommitUseCase,
) {
    private val _selectedItem = MutableStateFlow<SelectedItem>(SelectedItem.UncommitedChanges)
    val selectedItem: StateFlow<SelectedItem> = _selectedItem
    private val _taskEvent = MutableSharedFlow<TaskEvent>()
    val taskEvent: SharedFlow<TaskEvent> = _taskEvent
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

    private val _refreshData = MutableSharedFlow<RefreshType>()
    val refreshData: SharedFlow<RefreshType> = _refreshData

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
        showError: Boolean = true,
        refreshType: RefreshType,
        // TODO Eventually the title and subtitles should be mandatory but for now the default it's empty to slowly
        //  migrate the code that uses this function
        title: String = "",
        subtitle: String = "",
        // TODO For now have it always as false because the data refresh is cancelled even when the git process couldn't be cancelled
        isCancellable: Boolean = false,
        refreshEvenIfCrashes: Boolean = false,
        refreshEvenIfCrashesInteractive: ((Exception) -> Boolean)? = null,
        callback: suspend ProcessingInfo.(git: Git) -> Unit
    ): Job {
        val job = scope.launch(Dispatchers.IO) {
            var hasProcessFailed = false
            var refreshEvenIfCrashesInteractiveResult = false
            operationRunning = true

            val processingInfo: ProcessingInfo = object : ProcessingInfo {
                override fun changeSubtitle(newSubtitle: String) {
                    _processing.update { processingState ->
                        if (processingState is ProcessingState.Processing) {
                            processingState.copy(subtitle = newSubtitle)
                        } else {
                            ProcessingState.Processing(
                                title = title,
                                isCancellable = isCancellable,
                                subtitle = newSubtitle
                            )
                        }
                    }
                }

                override fun changeIsCancellable(newIsCancellable: Boolean) {
                    _processing.update { processingState ->
                        if (processingState is ProcessingState.Processing) {
                            processingState.copy(isCancellable = newIsCancellable)
                        } else {
                            ProcessingState.Processing(
                                title = title,
                                isCancellable = newIsCancellable,
                                subtitle = subtitle
                            )
                        }
                    }
                }
            }

            try {
                delayedStateChange(
                    delayMs = 300,
                    onDelayTriggered = {
                        _processing.update { processingState ->
                            if (processingState is ProcessingState.None) {
                                ProcessingState.Processing(title, subtitle, isCancellable)
                            } else {
                                processingState
                            }
                        }
                    }
                ) {
                    processingInfo.callback(git)
                }
            } catch (ex: Exception) {
                hasProcessFailed = true
                ex.printStackTrace()

                refreshEvenIfCrashesInteractiveResult = refreshEvenIfCrashesInteractive?.invoke(ex) ?: false

                val containsCancellation = exceptionContainsCancellation(ex)

                if (showError && !containsCancellation)
                    errorsManager.addError(newErrorNow(ex, null, ex.message.orEmpty()))

                printError(TAG, ex.message.orEmpty(), ex)
            } finally {
                _processing.value = ProcessingState.None
                operationRunning = false
                lastOperation = System.currentTimeMillis()

                if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes || refreshEvenIfCrashesInteractiveResult)) {
                    _refreshData.emit(refreshType)
                }
            }
        }

        this.currentJob = job

        return job
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
        showError: Boolean = true,
        // TODO Eventually the title and subtitles should be mandatory but for now the default it's empty to slowly
        //  migrate the code that uses this function
        title: String = "",
        subtitle: String = "",
        isCancellable: Boolean = false,
        callback: suspend CoroutineScope.() -> Unit
    ): Job {
        val job = scope.launch(Dispatchers.IO) {
            _processing.value = ProcessingState.Processing(title, subtitle, isCancellable)
            operationRunning = true

            try {
                this.callback()
            } catch (ex: Exception) {
                ex.printStackTrace()

                val containsCancellation = exceptionContainsCancellation(ex)

                if (showError && !containsCancellation)
                    errorsManager.addError(newErrorNow(ex, null, ex.localizedMessage))

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
        block: suspend (git: Git) -> Unit
    ) = scope.launch(Dispatchers.IO) {
        var hasProcessFailed = false

        operationRunning = true

        try {
            block(git)
        } catch (ex: Exception) {
            ex.printStackTrace()

            hasProcessFailed = true

            if (showError)
                errorsManager.addError(newErrorNow(ex, null, ex.localizedMessage))

            printError(TAG, ex.message.orEmpty(), ex)
        } finally {
            if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes))
                _refreshData.emit(refreshType)

            operationRunning = false
            lastOperation = System.currentTimeMillis()
        }
    }

    suspend fun refreshData(refreshType: RefreshType) {
        _refreshData.emit(refreshType)
    }

    suspend fun newSelectedStash(stash: RevCommit) {
        newSelectedItem(SelectedItem.Stash(stash))
    }

    suspend fun noneSelected() {
        newSelectedItem(SelectedItem.None)
    }

    fun newSelectedCommit(revCommit: RevCommit?) = runOperation(
        refreshType = RefreshType.NONE,
    ) { _ ->
        if (revCommit == null) {
            newSelectedItem(SelectedItem.None)
        } else {
            val newSelectedItem = SelectedItem.Commit(revCommit)
            newSelectedItem(newSelectedItem)
        }
    }

    fun newSelectedRef(objectId: ObjectId?) = runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        if (objectId == null) {
            newSelectedItem(SelectedItem.None)
        } else {
            val commit = findCommitUseCase(git, objectId)

            if (commit == null) {
                newSelectedItem(SelectedItem.None)
            } else {
                val newSelectedItem = SelectedItem.Ref(commit)
                newSelectedItem(newSelectedItem)
                _taskEvent.emit(TaskEvent.ScrollToGraphItem(newSelectedItem))
            }
        }
    }

    suspend fun newSelectedItem(selectedItem: SelectedItem, scrollToItem: Boolean = false) {
        _selectedItem.value = selectedItem

        if (scrollToItem) {
            _taskEvent.emit(TaskEvent.ScrollToGraphItem(selectedItem))
        }
    }

    suspend fun emitNewTaskEvent(taskEvent: TaskEvent) {
        _taskEvent.emit(taskEvent)
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
                    errorsManager.addError(newErrorNow(ex, null, ex.localizedMessage))
                }
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
    UNCOMMITED_CHANGES,
    UNCOMMITED_CHANGES_AND_LOG,
    REMOTES,
    REBASE_INTERACTIVE_STATE,
}
