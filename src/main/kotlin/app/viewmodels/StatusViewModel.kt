package app.viewmodels

import app.git.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import java.io.File
import javax.inject.Inject

class StatusViewModel @Inject constructor(
    private val tabState: TabState,
    private val statusManager: StatusManager,
    private val branchesManager: BranchesManager,
    private val repositoryManager: RepositoryManager,
    private val rebaseManager: RebaseManager,
    private val mergeManager: MergeManager,
) {
    private val _stageStatus = MutableStateFlow<StageStatus>(StageStatus.Loaded(listOf(), listOf()))
    val stageStatus: StateFlow<StageStatus> = _stageStatus

    private val _commitMessage = MutableStateFlow("")
    val commitMessage: StateFlow<String> = _commitMessage
    var newCommitMessage: String
        get() = commitMessage.value
        set(value) {
            _commitMessage.value = value
        }

    private var lastUncommitedChangesState = false

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

    fun resetUnstaged(diffEntry: DiffEntry) = tabState.runOperation { git ->
        statusManager.reset(git, diffEntry, staged = false)

        return@runOperation RefreshType.UNCOMMITED_CHANGES
    }

    private suspend fun loadStatus(git: Git) {
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

    private suspend fun loadHasUncommitedChanges(git: Git) = withContext(Dispatchers.IO) {
        lastUncommitedChangesState = statusManager.hasUncommitedChanges(git)
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
        val hadUncommitedChanges = this.lastUncommitedChangesState

        loadStatus(git)
        loadHasUncommitedChanges(git)

        val hasNowUncommitedChanges = this.lastUncommitedChangesState

        // Return true to update the log only if the uncommitedChanges status has changed
        return (hasNowUncommitedChanges != hadUncommitedChanges)
    }

    fun continueRebase() = tabState.safeProcessing { git ->
        rebaseManager.continueRebase(git)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun abortRebase() = tabState.safeProcessing { git ->
        rebaseManager.abortRebase(git)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun skipRebase() = tabState.safeProcessing { git ->
        rebaseManager.skipRebase(git)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun abortMerge() = tabState.safeProcessing { git ->
        mergeManager.abortMerge(git)

        return@safeProcessing RefreshType.ALL_DATA
    }

    fun deleteFile(diffEntry: DiffEntry) = tabState.runOperation { git ->
        val path = diffEntry.newPath

        val fileToDelete = File(git.repository.directory.parent, path)

        fileToDelete.delete()

        return@runOperation RefreshType.UNCOMMITED_CHANGES
    }
}

sealed class StageStatus {
    object Loading : StageStatus()
    data class Loaded(val staged: List<StatusEntry>, val unstaged: List<StatusEntry>) : StageStatus()
}

