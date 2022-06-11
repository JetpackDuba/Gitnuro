package app.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import app.extensions.delayedStateChange
import app.extensions.isMerging
import app.git.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryState
import java.io.File
import javax.inject.Inject

private const val MIN_TIME_IN_MS_TO_SHOW_LOAD = 500L

class StatusViewModel @Inject constructor(
    private val tabState: TabState,
    private val statusManager: StatusManager,
    private val rebaseManager: RebaseManager,
    private val mergeManager: MergeManager,
    private val logManager: LogManager,
) {
    private val _stageStatus = MutableStateFlow<StageStatus>(StageStatus.Loaded(listOf(), listOf(), false))
    val stageStatus: StateFlow<StageStatus> = _stageStatus

    var savedCommitMessage = CommitMessage("", MessageType.NORMAL)

    var hasPreviousCommits = true // When false, disable "amend previous commit"

    private var lastUncommitedChangesState = false

    val stagedLazyListState = MutableStateFlow(LazyListState(0, 0))
    val unstagedLazyListState = MutableStateFlow(LazyListState(0, 0))

    /**
     * Notify the UI that the commit message has been changed by the view model
     */
    private val _commitMessageChangesFlow = MutableSharedFlow<String>()
    val commitMessageChangesFlow: SharedFlow<String> = _commitMessageChangesFlow

    private fun persistMessage() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val messageToPersist = savedCommitMessage.message.ifBlank { null }

        if (git.repository.repositoryState.isMerging) {
            git.repository.writeMergeCommitMsg(messageToPersist)
        } else if (git.repository.repositoryState == RepositoryState.SAFE) {
            git.repository.writeCommitEditMsg(messageToPersist)
        }
    }

    fun stage(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.stage(git, statusEntry)
    }

    fun unstage(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.unstage(git, statusEntry)
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

    fun resetStaged(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.reset(git, statusEntry, staged = true)
    }

    fun resetUnstaged(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.reset(git, statusEntry, staged = false)
    }

    private suspend fun loadStatus(git: Git) {
        val previousStatus = _stageStatus.value

        val requiredMessageType = if (git.repository.repositoryState == RepositoryState.MERGING) {
            MessageType.MERGE
        } else {
            MessageType.NORMAL
        }

        if (requiredMessageType != savedCommitMessage.messageType) {
            savedCommitMessage = CommitMessage(messageByRepoState(git), requiredMessageType)
            _commitMessageChangesFlow.emit(savedCommitMessage.message)

        } else if (savedCommitMessage.message.isEmpty()) {
            savedCommitMessage = savedCommitMessage.copy(message = messageByRepoState(git))
            _commitMessageChangesFlow.emit(savedCommitMessage.message)
        }

        try {
            delayedStateChange(
                delayMs = MIN_TIME_IN_MS_TO_SHOW_LOAD,
                onDelayTriggered = {
                    if (previousStatus is StageStatus.Loaded) {
                        _stageStatus.value = previousStatus.copy(isPartiallyReloading = true)
                    } else {
                        _stageStatus.value = StageStatus.Loading
                    }
                }
            ) {
                val status = statusManager.getStatus(git)
                val staged = statusManager.getStaged(status)
                val unstaged = statusManager.getUnstaged(status)

                _stageStatus.value = StageStatus.Loaded(staged, unstaged, isPartiallyReloading = false)
            }
        } catch (ex: Exception) {
            _stageStatus.value = previousStatus
            throw ex
        }
    }

    private fun messageByRepoState(git: Git): String {
        val message: String? = if (git.repository.repositoryState == RepositoryState.MERGING) {
            git.repository.readMergeCommitMsg()
        } else {
            git.repository.readCommitEditMsg()
        }

        //TODO this replace is a workaround until this issue gets fixed https://github.com/JetBrains/compose-jb/issues/615
        return message.orEmpty().replace("\t", "    ")
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
        updateCommitMessage("")
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

    fun deleteFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        val path = statusEntry.filePath

        val fileToDelete = File(git.repository.directory.parent, path)

        fileToDelete.delete()
    }

    fun updateCommitMessage(message: String) {
        savedCommitMessage = savedCommitMessage.copy(message = message)
        persistMessage()
    }
}

sealed class StageStatus {
    object Loading : StageStatus()
    data class Loaded(
        val staged: List<StatusEntry>,
        val unstaged: List<StatusEntry>,
        val isPartiallyReloading: Boolean
    ) : StageStatus()
}

data class CommitMessage(val message: String, val messageType: MessageType)

enum class MessageType {
    NORMAL,
    MERGE;
}