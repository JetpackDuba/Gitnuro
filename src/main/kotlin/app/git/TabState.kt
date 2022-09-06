package app.git

import app.ErrorsManager
import app.di.TabScope
import app.extensions.delayedStateChange
import app.newErrorNow
import app.ui.SelectedItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@TabScope
class TabState @Inject constructor(
    val errorsManager: ErrorsManager,
) {
    private val _selectedItem = MutableStateFlow<SelectedItem>(SelectedItem.None)
    val selectedItem: StateFlow<SelectedItem> = _selectedItem
    private val _taskEvent = MutableSharedFlow<TaskEvent>()
    val taskEvent: SharedFlow<TaskEvent> = _taskEvent

    var git: Git? = null
    private val safeGit: Git
        get() {
            val git = this.git
            if (git == null) {
                throw CancellationException("Null git object")
            } else
                return git
        }

//    private val mutex = Mutex()

    private val _refreshData = MutableSharedFlow<RefreshType>()
    val refreshData: Flow<RefreshType> = _refreshData

    val managerScope = CoroutineScope(SupervisorJob())

    /**
     * Property that indicates if a git operation is running
     */
    @set:Synchronized
    var operationRunning = false

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing

    fun safeProcessing(
        showError: Boolean = true,
        refreshType: RefreshType,
        refreshEvenIfCrashes: Boolean = false,
        callback: suspend (git: Git) -> Unit
    ) =
        managerScope.launch(Dispatchers.IO) {
            var hasProcessFailed = false
            operationRunning = true

            try {
                delayedStateChange(
                    delayMs = 300,
                    onDelayTriggered = {
                        _processing.value = true
                    }
                ) {
                    callback(safeGit)
                }
            } catch (ex: Exception) {
                hasProcessFailed = true
                ex.printStackTrace()

                if (showError)
                    errorsManager.addError(newErrorNow(ex, ex.message.orEmpty()))
            } finally {
                _processing.value = false
                operationRunning = false

                if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes))
                    _refreshData.emit(refreshType)
            }

        }

    fun safeProcessingWihoutGit(showError: Boolean = true, callback: suspend CoroutineScope.() -> Unit) =
        managerScope.launch(Dispatchers.IO) {
            _processing.value = true
            operationRunning = true

            try {
                this.callback()
            } catch (ex: Exception) {
                ex.printStackTrace()

                if (showError)
                    errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
            } finally {
                _processing.value = false
                operationRunning = false
            }
        }

    fun runOperation(
        showError: Boolean = false,
        refreshType: RefreshType,
        refreshEvenIfCrashes: Boolean = false,
        block: suspend (git: Git) -> Unit
    ) = managerScope.launch(Dispatchers.IO) {
        var hasProcessFailed = false

        operationRunning = true
        try {
            block(safeGit)

            if (refreshType != RefreshType.NONE)
                _refreshData.emit(refreshType)
        } catch (ex: Exception) {
            ex.printStackTrace()

            hasProcessFailed = true

            if (showError)
                errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
        } finally {
            launch {
                // Add a slight delay because sometimes the file watcher takes a few moments to notify a change in the
                // filesystem, therefore notifying late and being operationRunning already false (which leads to a full
                // refresh because there have been changes in the git dir). This can be easily triggered by interactive
                // rebase.
                delay(500)
                operationRunning = false
            }


            if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes))
                _refreshData.emit(refreshType)
        }
    }

    suspend fun coRunOperation(
        showError: Boolean = false,
        refreshType: RefreshType,
        refreshEvenIfCrashes: Boolean = false,
        block: suspend (git: Git) -> Unit
    ) = withContext(Dispatchers.IO) {
        var hasProcessFailed = false

        operationRunning = true
        try {
            block(safeGit)

            if (refreshType != RefreshType.NONE)
                _refreshData.emit(refreshType)
        } catch (ex: Exception) {
            ex.printStackTrace()

            hasProcessFailed = true

            if (showError)
                errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
        } finally {
            launch {
                // Add a slight delay because sometimes the file watcher takes a few moments to notify a change in the
                // filesystem, therefore notifying late and being operationRunning already false (which leads to a full
                // refresh because there have been changes in the git dir). This can be easily triggered by interactive
                // rebase.
                delay(500)
                operationRunning = false
            }


            if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes))
                _refreshData.emit(refreshType)
        }
    }

    suspend fun refreshData(refreshType: RefreshType) {
        _refreshData.emit(refreshType)
    }

    fun newSelectedStash(stash: RevCommit) {
        newSelectedItem(SelectedItem.Stash(stash))
    }

    fun noneSelected() {
        newSelectedItem(SelectedItem.None)
    }

    fun newSelectedRef(objectId: ObjectId?) = runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        if (objectId == null) {
            newSelectedItem(SelectedItem.None)
        } else {
            val commit = findCommit(git, objectId)
            newSelectedItem(SelectedItem.Ref(commit))
        }
    }

    private fun findCommit(git: Git, objectId: ObjectId): RevCommit {
        return git.repository.parseCommit(objectId)
    }

    fun newSelectedItem(selectedItem: SelectedItem) {
        _selectedItem.value = selectedItem
    }

    suspend fun emitNewTaskEvent(taskEvent: TaskEvent) {
        _taskEvent.emit(taskEvent)
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
}