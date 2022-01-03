package app.viewmodels

import app.git.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class StatusViewModel @Inject constructor(
    private val tabState: TabState,
    private val statusManager: StatusManager,
    private val branchesManager: BranchesManager,
    private val repositoryManager: RepositoryManager,
) {
    private val _stageStatus = MutableStateFlow<StageStatus>(StageStatus.Loaded(listOf(), listOf()))
    val stageStatus: StateFlow<StageStatus> = _stageStatus


    private val _hasUncommitedChanges = MutableStateFlow<Boolean>(false)
    val hasUncommitedChanges: StateFlow<Boolean>
        get() = _hasUncommitedChanges

    fun stage(diffEntry: DiffEntry) = tabState.runOperation { git ->
        statusManager.stage(git, diffEntry)

        return@runOperation RefreshType.UNCOMMITED_CHANGES
    }

    fun unstage(diffEntry: DiffEntry) = tabState.runOperation { git ->
        statusManager.unstage(git, diffEntry)

        return@runOperation RefreshType.UNCOMMITED_CHANGES
    }



    fun unstageAll() = tabState.safeProcessing { git ->
        statusManager.unstageAll(git)

        return@safeProcessing RefreshType.UNCOMMITED_CHANGES
    }

    fun stageAll() = tabState.safeProcessing { git ->
        statusManager.stageAll(git)

        return@safeProcessing RefreshType.UNCOMMITED_CHANGES
    }



    fun resetStaged(diffEntry: DiffEntry) = tabState.runOperation { git ->
        statusManager.reset(git, diffEntry, staged = true)

        return@runOperation RefreshType.UNCOMMITED_CHANGES
    }

    fun resetUnstaged(diffEntry: DiffEntry) =tabState.runOperation { git ->
        statusManager.reset(git, diffEntry, staged = false)

        return@runOperation RefreshType.UNCOMMITED_CHANGES
    }

    suspend fun loadStatus(git: Git) {
        val previousStatus = _stageStatus.value

        try {
            _stageStatus.value = StageStatus.Loading
            val repositoryState = repositoryManager.getRepositoryState(git)
            val currentBranchRef = branchesManager.currentBranchRef(git)
            val staged = statusManager.getStaged(git, currentBranchRef, repositoryState)
            val unstaged = statusManager.getUnstaged(git, repositoryState)

            _stageStatus.value = StageStatus.Loaded(staged, unstaged)
        } catch (ex: Exception) {
            _stageStatus.value = previousStatus
            throw ex
        }
    }

    suspend fun loadHasUncommitedChanges(git: Git) = withContext(Dispatchers.IO) {
        _hasUncommitedChanges.value = statusManager.hasUncommitedChanges(git)
    }

    fun commit(message: String) = tabState.safeProcessing { git ->
        statusManager.commit(git, message)

        return@safeProcessing RefreshType.ALL_DATA
    }



    suspend fun refresh(git: Git) = withContext(Dispatchers.IO) {
        loadStatus(git)
        loadHasUncommitedChanges(git)
    }

    /**
     * Checks if there are uncommited changes and returns if the state has changed (
     */
    suspend fun updateHasUncommitedChanges(git: Git): Boolean {
        val hadUncommitedChanges = hasUncommitedChanges.value

        loadStatus(git)

        val hasNowUncommitedChanges = hasUncommitedChanges.value

        // Return true to update the log only if the uncommitedChanges status has changed
        return (hasNowUncommitedChanges != hadUncommitedChanges)
    }
}

sealed class StageStatus {
    object Loading : StageStatus()
    data class Loaded(val staged: List<StatusEntry>, val unstaged: List<StatusEntry>) : StageStatus()
}

