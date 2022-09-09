package app.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import app.extensions.delayedStateChange
import app.extensions.isMerging
import app.extensions.isReverting
import app.git.*
import app.git.log.CheckHasPreviousCommitsUseCase
import app.git.log.GetLastCommitMessageUseCase
import app.git.rebase.AbortRebaseUseCase
import app.git.rebase.ContinueRebaseUseCase
import app.git.rebase.SkipRebaseUseCase
import app.git.repository.ResetRepositoryStateUseCase
import app.git.workspace.*
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
    private val stageEntryUseCase: StageEntryUseCase,
    private val unstageEntryUseCase: UnstageEntryUseCase,
    private val resetEntryUseCase: ResetEntryUseCase,
    private val stageAllUseCase: StageAllUseCase,
    private val unstageAllUseCase: UnstageAllUseCase,
    private val checkHasPreviousCommitsUseCase: CheckHasPreviousCommitsUseCase,
    private val getLastCommitMessageUseCase: GetLastCommitMessageUseCase,
    private val resetRepositoryStateUseCase: ResetRepositoryStateUseCase,
    private val continueRebaseUseCase: ContinueRebaseUseCase,
    private val abortRebaseUseCase: AbortRebaseUseCase,
    private val skipRebaseUseCase: SkipRebaseUseCase,
    private val getStatusUseCase: GetStatusUseCase,
    private val getStagedUseCase: GetStagedUseCase,
    private val getUnstagedUseCase: GetUnstagedUseCase,
    private val checkHasUncommitedChangedUseCase: CheckHasUncommitedChangedUseCase,
    private val doCommitUseCase: DoCommitUseCase,
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

        if (git.repository.repositoryState.isMerging ||
            git.repository.repositoryState.isRebasing ||
            git.repository.repositoryState.isReverting
        ) {
            git.repository.writeMergeCommitMsg(messageToPersist)
        } else if (git.repository.repositoryState == RepositoryState.SAFE) {
            git.repository.writeCommitEditMsg(messageToPersist)
        }
    }

    fun stage(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
        showError = true,
    ) { git ->
        stageEntryUseCase(git, statusEntry)
    }

    fun unstage(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
        showError = true,
    ) { git ->
        unstageEntryUseCase(git, statusEntry)
    }


    fun unstageAll() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        unstageAllUseCase(git)
    }

    fun stageAll() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        stageAllUseCase(git)
    }

    fun resetStaged(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        resetEntryUseCase(git, statusEntry, staged = true)
    }

    fun resetUnstaged(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        resetEntryUseCase(git, statusEntry, staged = false)
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
                val status = getStatusUseCase(git)
                val staged = getStagedUseCase(status).sortedBy { it.filePath }
                val unstaged = getUnstagedUseCase(status).sortedBy { it.filePath }

                _stageStatus.value = StageStatus.Loaded(staged, unstaged, isPartiallyReloading = false)
            }
        } catch (ex: Exception) {
            _stageStatus.value = previousStatus
            throw ex
        }
    }

    private fun messageByRepoState(git: Git): String {
        val message: String? = if (
            git.repository.repositoryState.isMerging ||
            git.repository.repositoryState.isRebasing ||
            git.repository.repositoryState.isReverting
        ) {
            git.repository.readMergeCommitMsg()
        } else {
            git.repository.readCommitEditMsg()
        }

        //TODO this replace is a workaround until this issue gets fixed https://github.com/JetBrains/compose-jb/issues/615
        return message.orEmpty().replace("\t", "    ")
    }

    private suspend fun loadHasUncommitedChanges(git: Git) = withContext(Dispatchers.IO) {
        lastUncommitedChangesState = checkHasUncommitedChangedUseCase(git)
    }

    fun commit(message: String, amend: Boolean) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        val commitMessage = if (amend && message.isBlank()) {
            getLastCommitMessageUseCase(git)
        } else
            message

        doCommitUseCase(git, commitMessage, amend)
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
        hasPreviousCommits = checkHasPreviousCommitsUseCase(git)

        // Return true to update the log only if the uncommitedChanges status has changed
        return (hasNowUncommitedChanges != hadUncommitedChanges)
    }

    fun continueRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        continueRebaseUseCase(git)
    }

    fun abortRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        abortRebaseUseCase(git)
    }

    fun skipRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        skipRebaseUseCase(git)
    }

    fun resetRepoState() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        resetRepositoryStateUseCase(git)
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