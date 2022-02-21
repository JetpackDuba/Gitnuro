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
    private val logManager: LogManager,
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

    var hasPreviousCommits = true // When false, disable "amend previous commit"

    private var lastUncommitedChangesState = false

    fun stage(diffEntry: DiffEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.stage(git, diffEntry)
    }

    fun unstage(diffEntry: DiffEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.unstage(git, diffEntry)
    }


    fun unstageAll() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.unstageAll(git)
    }

    fun stageAll() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.stageAll(git)
    }

    fun resetStaged(diffEntry: DiffEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.reset(git, diffEntry, staged = true)
    }

    fun resetUnstaged(diffEntry: DiffEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.reset(git, diffEntry, staged = false)
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

    fun commit(message: String, amend: Boolean) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        val commitMessage = if (amend && message.isBlank()) {
            logManager.latestMessage(git)
        } else
            message

        statusManager.commit(git, commitMessage, amend)
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
        hasPreviousCommits = logManager.hasPreviousCommits(git)

        // Return true to update the log only if the uncommitedChanges status has changed
        return (hasNowUncommitedChanges != hadUncommitedChanges)
    }

    fun continueRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        rebaseManager.continueRebase(git)
    }

    fun abortRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        rebaseManager.abortRebase(git)
    }

    fun skipRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        rebaseManager.skipRebase(git)
    }

    fun abortMerge() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        mergeManager.abortMerge(git)
    }

    fun deleteFile(diffEntry: DiffEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        val path = diffEntry.newPath

        val fileToDelete = File(git.repository.directory.parent, path)

        fileToDelete.delete()
    }
}

sealed class StageStatus {
    object Loading : StageStatus()
    data class Loaded(val staged: List<StatusEntry>, val unstaged: List<StatusEntry>) : StageStatus()
}

