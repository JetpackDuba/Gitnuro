package app.git

import app.ErrorsManager
import app.di.TabScope
import app.newErrorNow
import app.ui.SelectedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    var git: Git? = null
    val safeGit: Git
        get() {
            val git = this.git
            if (git == null) {
                throw CancellationException("Null git object")
            } else
                return git
        }

    val mutex = Mutex()

    private val _refreshData = MutableSharedFlow<RefreshType>()
    val refreshData: Flow<RefreshType> = _refreshData

    val managerScope = CoroutineScope(SupervisorJob())


    /**
     * Property that indicates if a git operation is running
     */
    @set:Synchronized
    var operationRunning = false
        get() {
            return field || mutex.isLocked
        }

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing

    fun safeProcessing(
        showError: Boolean = true,
        refreshType: RefreshType,
        refreshEvenIfCrashes: Boolean = false,
        callback: suspend (git: Git) -> Unit
    ) =
        managerScope.launch(Dispatchers.IO) {
            mutex.withLock {
                var hasProcessFailed = false
                _processing.value = true

                try {
                    callback(safeGit)
                } catch (ex: Exception) {
                    hasProcessFailed = true
                    ex.printStackTrace()

                    if (showError)
                        errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
                } finally {
                    _processing.value = false

                    if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes))
                        _refreshData.emit(refreshType)
                }
            }
        }

    fun safeProcessingWihoutGit(showError: Boolean = true, callback: suspend () -> Unit) =
        managerScope.launch(Dispatchers.IO) {
            mutex.withLock {
                _processing.value = true
                operationRunning = true

                try {
                    callback()
                } catch (ex: Exception) {
                    ex.printStackTrace()

                    if (showError)
                        errorsManager.addError(newErrorNow(ex, ex.localizedMessage))
                } finally {
                    _processing.value = false
                    operationRunning = false
                }
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
            operationRunning = false

            if (refreshType != RefreshType.NONE && (!hasProcessFailed || refreshEvenIfCrashes))
                _refreshData.emit(refreshType)
        }
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
}

enum class RefreshType {
    NONE,
    ALL_DATA,
    ONLY_LOG,
    STASHES,
    UNCOMMITED_CHANGES,
    UNCOMMITED_CHANGES_AND_LOG,
    REMOTES,
}