package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.extensions.isMerging
import com.jetpackduba.gitnuro.extensions.isReverting
import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.author.LoadAuthorUseCase
import com.jetpackduba.gitnuro.git.author.SaveAuthorUseCase
import com.jetpackduba.gitnuro.git.log.CheckHasPreviousCommitsUseCase
import com.jetpackduba.gitnuro.git.log.GetLastCommitMessageUseCase
import com.jetpackduba.gitnuro.git.log.GetSpecificCommitMessageUseCase
import com.jetpackduba.gitnuro.git.rebase.*
import com.jetpackduba.gitnuro.git.repository.GetRepositoryStateUseCase
import com.jetpackduba.gitnuro.git.repository.ResetRepositoryStateUseCase
import com.jetpackduba.gitnuro.git.workspace.*
import com.jetpackduba.gitnuro.models.AuthorInfo
import com.jetpackduba.gitnuro.preferences.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
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
    private val checkHasUncommitedChangesUseCase: CheckHasUncommitedChangesUseCase,
    private val doCommitUseCase: DoCommitUseCase,
    private val loadAuthorUseCase: LoadAuthorUseCase,
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val getRepositoryStateUseCase: GetRepositoryStateUseCase,
    private val getRebaseAmendCommitIdUseCase: GetRebaseAmendCommitIdUseCase,
    private val sharedRepositoryStateManager: SharedRepositoryStateManager,
    private val getSpecificCommitMessageUseCase: GetSpecificCommitMessageUseCase,
    private val tabScope: CoroutineScope,
    private val appSettings: AppSettings,
) {
    private val _showSearchUnstaged = MutableStateFlow(false)
    val showSearchUnstaged: StateFlow<Boolean> = _showSearchUnstaged

    private val _showSearchStaged = MutableStateFlow(false)
    val showSearchStaged: StateFlow<Boolean> = _showSearchStaged

    private val _searchFilterUnstaged = MutableStateFlow(TextFieldValue(""))
    val searchFilterUnstaged: StateFlow<TextFieldValue> = _searchFilterUnstaged

    private val _searchFilterStaged = MutableStateFlow(TextFieldValue(""))
    val searchFilterStaged: StateFlow<TextFieldValue> = _searchFilterStaged

    val swapUncommitedChanges = appSettings.swapUncommitedChangesFlow
    val rebaseInteractiveState = sharedRepositoryStateManager.rebaseInteractiveState

    private val _stageState = MutableStateFlow<StageState>(StageState.Loading)

    val stageState: StateFlow<StageState> = combine(
        _stageState,
        _showSearchStaged,
        _searchFilterStaged,
        _showSearchUnstaged,
        _searchFilterUnstaged,
    ) { state, showSearchStaged, filterStaged, showSearchUnstaged, filterUnstaged ->
        if (state is StageState.Loaded) {
            val unstaged = if (showSearchUnstaged && filterUnstaged.text.isNotBlank()) {
                state.unstaged.filter { it.filePath.lowercaseContains(filterUnstaged.text) }
            } else {
                state.unstaged
            }

            val staged = if (showSearchStaged && filterStaged.text.isNotBlank()) {
                state.staged.filter { it.filePath.lowercaseContains(filterStaged.text) }
            } else {
                state.staged
            }

            state.copy(stagedFiltered = staged, unstagedFiltered = unstaged)

        } else {
            state
        }
    }.stateIn(
        tabScope,
        SharingStarted.Lazily,
        StageState.Loading
    )

    var savedCommitMessage = CommitMessage("", MessageType.NORMAL)

    var hasPreviousCommits = true // When false, disable "amend previous commit"

    private var lastUncommitedChangesState = false

    val stagedLazyListState = MutableStateFlow(LazyListState(0, 0))
    val unstagedLazyListState = MutableStateFlow(LazyListState(0, 0))

    private val _committerDataRequestState = MutableStateFlow<CommitterDataRequestState>(CommitterDataRequestState.None)
    val committerDataRequestState: StateFlow<CommitterDataRequestState> = _committerDataRequestState

    /**
     * Notify the UI that the commit message has been changed by the view model
     */
    private val _commitMessageChangesFlow = MutableSharedFlow<String>()
    val commitMessageChangesFlow: SharedFlow<String> = _commitMessageChangesFlow

    private val _isAmend = MutableStateFlow(false)
    val isAmend: StateFlow<Boolean> = _isAmend

    private val _isAmendRebaseInteractive = MutableStateFlow(true) // TODO should copy message from previous commit when this is required
    val isAmendRebaseInteractive: StateFlow<Boolean> = _isAmendRebaseInteractive

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(
                RefreshType.ALL_DATA,
                RefreshType.UNCOMMITED_CHANGES,
                RefreshType.UNCOMMITED_CHANGES_AND_LOG,
            ) {
                refresh(tabState.git)
            }
        }
    }

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
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        resetEntryUseCase(git, statusEntry, staged = true)
    }

    fun resetUnstaged(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
    ) { git ->
        resetEntryUseCase(git, statusEntry, staged = false)
    }

    private suspend fun loadStatus(git: Git) {
        val previousStatus = _stageState.value

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
                    if (previousStatus is StageState.Loaded) {
                        _stageState.value = previousStatus.copy(isPartiallyReloading = true)
                    } else {
                        _stageState.value = StageState.Loading
                    }
                }
            ) {
                val status = getStatusUseCase(git)
                val staged = getStagedUseCase(status).sortedBy { it.filePath }
                val unstaged = getUnstagedUseCase(status).sortedBy { it.filePath }

                _stageState.value = StageState.Loaded(
                    staged = staged,
                    stagedFiltered = staged,
                    unstaged = unstaged,
                    unstagedFiltered = unstaged, isPartiallyReloading = false
                )
            }
        } catch (ex: Exception) {
            _stageState.value = previousStatus
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
        lastUncommitedChangesState = checkHasUncommitedChangesUseCase(git)
    }

    fun amend(isAmend: Boolean) {
        _isAmend.value = isAmend

        if (isAmend && savedCommitMessage.message.isEmpty()) {
            takeMessageFromPreviousCommit()
        }
    }

    fun amendRebaseInteractive(isAmend: Boolean) {
        _isAmendRebaseInteractive.value = isAmend

        if (isAmend && savedCommitMessage.message.isEmpty()) {
            takeMessageFromAmendCommit()
        }
    }

    fun commit(message: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        val amend = isAmend.value

        val commitMessage = if (amend && message.isBlank()) {
            getLastCommitMessageUseCase(git)
        } else
            message


        val personIdent = getPersonIdent(git)

        doCommitUseCase(git, commitMessage, amend, personIdent)
        updateCommitMessage("")
        _isAmend.value = false
    }

    private suspend fun getPersonIdent(git: Git): PersonIdent? {
        val author = loadAuthorUseCase(git)

        return if (
            author.name.isNullOrEmpty() && author.globalName.isNullOrEmpty() ||
            author.email.isNullOrEmpty() && author.globalEmail.isNullOrEmpty()
        ) {
            _committerDataRequestState.value = CommitterDataRequestState.WaitingInput(author)

            var committerData = _committerDataRequestState.value

            while (committerData is CommitterDataRequestState.WaitingInput) {
                committerData = _committerDataRequestState.value
            }

            if (committerData is CommitterDataRequestState.Accepted) {
                val authorInfo = committerData.authorInfo

                if (committerData.persist) {
                    saveAuthorUseCase(git, authorInfo)
                }

                PersonIdent(authorInfo.globalName, authorInfo.globalEmail)
            } else {
                null
            }
        } else
            null
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

    fun continueRebase(message: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        val repositoryState = sharedRepositoryStateManager.repositoryState.value
        val rebaseInteractiveState = sharedRepositoryStateManager.rebaseInteractiveState.value

        if (
            repositoryState == RepositoryState.REBASING_INTERACTIVE &&
            rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits &&
            rebaseInteractiveState.isCurrentStepAmenable &&
            isAmendRebaseInteractive.value
        ) {
            val amendCommitId = rebaseInteractiveState.commitToAmendId

            if (!amendCommitId.isNullOrBlank()) {
                doCommitUseCase(git, message, true, getPersonIdent(git))
            }
        }

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

        val fileToDelete = File(git.repository.workTree, path)

        fileToDelete.deleteRecursively()
    }

    fun updateCommitMessage(message: String) {
        savedCommitMessage = savedCommitMessage.copy(message = message)
        persistMessage()
    }

    private fun takeMessageFromPreviousCommit() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        savedCommitMessage = savedCommitMessage.copy(message = getLastCommitMessageUseCase(git))
        persistMessage()
        _commitMessageChangesFlow.emit(savedCommitMessage.message)
    }

    private fun takeMessageFromAmendCommit() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val rebaseInteractiveState = rebaseInteractiveState.value
        if (rebaseInteractiveState !is RebaseInteractiveState.ProcessingCommits) {
            return@runOperation
        }

        val commitId = rebaseInteractiveState.commitToAmendId ?: return@runOperation
        val message = getSpecificCommitMessageUseCase(git, commitId)

        savedCommitMessage = savedCommitMessage.copy(message = message)
        persistMessage()
        _commitMessageChangesFlow.emit(savedCommitMessage.message)
    }

    fun onRejectCommitterData() {
        this._committerDataRequestState.value = CommitterDataRequestState.Reject
    }

    fun onAcceptCommitterData(newAuthorInfo: AuthorInfo, persist: Boolean) {
        this._committerDataRequestState.value = CommitterDataRequestState.Accepted(newAuthorInfo, persist)
    }

    fun onSearchFilterToggledStaged(visible: Boolean) {
        _showSearchStaged.value = visible
    }

    fun onSearchFilterChangedStaged(filter: TextFieldValue) {
        _searchFilterStaged.value = filter
    }

    fun onSearchFilterToggledUnstaged(visible: Boolean) {
        _showSearchUnstaged.value = visible
    }

    fun onSearchFilterChangedUnstaged(filter: TextFieldValue) {
        _searchFilterUnstaged.value = filter
    }
}

sealed class StageState {
    object Loading : StageState()
    data class Loaded(
        val staged: List<StatusEntry>,
        val stagedFiltered: List<StatusEntry>,
        val unstaged: List<StatusEntry>,
        val unstagedFiltered: List<StatusEntry>,
        val isPartiallyReloading: Boolean
    ) : StageState()
}

data class CommitMessage(val message: String, val messageType: MessageType)

enum class MessageType {
    NORMAL,
    MERGE;
}

sealed interface CommitterDataRequestState {
    object None : CommitterDataRequestState
    data class WaitingInput(val authorInfo: AuthorInfo) : CommitterDataRequestState
    data class Accepted(val authorInfo: AuthorInfo, val persist: Boolean) : CommitterDataRequestState
    object Reject : CommitterDataRequestState
}
