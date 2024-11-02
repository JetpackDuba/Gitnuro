package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.CloseableView
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.author.LoadAuthorUseCase
import com.jetpackduba.gitnuro.git.author.SaveAuthorUseCase
import com.jetpackduba.gitnuro.git.log.GetLastCommitMessageUseCase
import com.jetpackduba.gitnuro.git.log.GetSpecificCommitMessageUseCase
import com.jetpackduba.gitnuro.git.rebase.AbortRebaseUseCase
import com.jetpackduba.gitnuro.git.rebase.ContinueRebaseUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.git.rebase.SkipRebaseUseCase
import com.jetpackduba.gitnuro.git.repository.ResetRepositoryStateUseCase
import com.jetpackduba.gitnuro.git.workspace.*
import com.jetpackduba.gitnuro.models.AuthorInfo
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import com.jetpackduba.gitnuro.ui.tree_files.entriesToTreeEntry
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
    private val stageByDirectoryUseCase: StageByDirectoryUseCase,
    private val unstageByDirectoryUseCase: UnstageByDirectoryUseCase,
    private val resetEntryUseCase: DiscardEntryUseCase,
    private val stageAllUseCase: StageAllUseCase,
    private val unstageAllUseCase: UnstageAllUseCase,
    private val getLastCommitMessageUseCase: GetLastCommitMessageUseCase,
    private val resetRepositoryStateUseCase: ResetRepositoryStateUseCase,
    private val continueRebaseUseCase: ContinueRebaseUseCase,
    private val abortRebaseUseCase: AbortRebaseUseCase,
    private val skipRebaseUseCase: SkipRebaseUseCase,
    private val getStatusUseCase: GetStatusUseCase,
    private val getStagedUseCase: GetStagedUseCase,
    private val getUnstagedUseCase: GetUnstagedUseCase,
    private val checkHasUncommittedChangesUseCase: CheckHasUncommittedChangesUseCase,
    private val doCommitUseCase: DoCommitUseCase,
    private val loadAuthorUseCase: LoadAuthorUseCase,
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val sharedRepositoryStateManager: SharedRepositoryStateManager,
    private val getSpecificCommitMessageUseCase: GetSpecificCommitMessageUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val tabScope: CoroutineScope,
) {
    private val _showSearchUnstaged = MutableStateFlow(false)
    val showSearchUnstaged: StateFlow<Boolean> = _showSearchUnstaged

    private val _showSearchStaged = MutableStateFlow(false)
    val showSearchStaged: StateFlow<Boolean> = _showSearchStaged

    private val _searchFilterUnstaged = MutableStateFlow(TextFieldValue(""))
    val searchFilterUnstaged: StateFlow<TextFieldValue> = _searchFilterUnstaged

    private val _searchFilterStaged = MutableStateFlow(TextFieldValue(""))
    val searchFilterStaged: StateFlow<TextFieldValue> = _searchFilterStaged

    val swapUncommittedChanges = appSettingsRepository.swapUncommittedChangesFlow
    val rebaseInteractiveState = sharedRepositoryStateManager.rebaseInteractiveState

    private val treeContractedDirectories = MutableStateFlow(emptyList<String>())
    private val showAsTree = appSettingsRepository.showChangesAsTreeFlow
    private val _stageState = MutableStateFlow<StageState>(StageState.Loading)

    private val stageStateFiltered: StateFlow<StageState> = combine(
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
            }.prioritizeConflicts()

            val staged = if (showSearchStaged && filterStaged.text.isNotBlank()) {
                state.staged.filter { it.filePath.lowercaseContains(filterStaged.text) }
            } else {
                state.staged
            }.prioritizeConflicts()

            state.copy(staged = staged, unstaged = unstaged)

        } else {
            state
        }
    }
        .stateIn(
            tabScope,
            SharingStarted.Lazily,
            StageState.Loading
        )


    val stageStateUi: StateFlow<StageStateUi> = combine(
        stageStateFiltered,
        showAsTree,
        treeContractedDirectories,
    ) { stageStateFiltered, showAsTree, contractedDirectories ->
        when (stageStateFiltered) {
            is StageState.Loaded -> {
                if (showAsTree) {
                    StageStateUi.TreeLoaded(
                        staged = entriesToTreeEntry(stageStateFiltered.staged, contractedDirectories) { it.filePath },
                        unstaged = entriesToTreeEntry(
                            stageStateFiltered.unstaged,
                            contractedDirectories
                        ) { it.filePath },
                        isPartiallyReloading = stageStateFiltered.isPartiallyReloading,
                    )
                } else {
                    StageStateUi.ListLoaded(
                        staged = stageStateFiltered.staged,
                        unstaged = stageStateFiltered.unstaged,
                        isPartiallyReloading = stageStateFiltered.isPartiallyReloading,
                    )
                }
            }

            StageState.Loading -> StageStateUi.Loading
        }
    }
        .stateIn(
            tabScope,
            SharingStarted.Lazily,
            StageStateUi.Loading
        )

    var savedCommitMessage = CommitMessage("", MessageType.NORMAL)

    var hasPreviousCommits = true // When false, disable "amend previous commit"

    private var lastUncommittedChangesState = false

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

    private val _isAmendRebaseInteractive =
        MutableStateFlow(true) // TODO should copy message from previous commit when this is required
    val isAmendRebaseInteractive: StateFlow<Boolean> = _isAmendRebaseInteractive

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(
                RefreshType.ALL_DATA,
                RefreshType.UNCOMMITTED_CHANGES,
                RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
            ) {
                refresh(tabState.git)
            }
        }

        tabScope.launch {
            showSearchStaged.collectLatest {
                if (it) {
                    addStagedSearchToCloseableView()
                } else {
                    removeStagedSearchToCloseableView()
                }
            }
        }

        tabScope.launch {
            showSearchUnstaged.collectLatest {
                if (it) {
                    addUnstagedSearchToCloseableView()
                } else {
                    removeUnstagedSearchToCloseableView()
                }
            }
        }

        tabScope.launch {
            tabState.closeViewFlow.collectLatest {
                if (it == CloseableView.STAGED_CHANGES_SEARCH) {
                    onSearchFilterToggledStaged(false)
                } else if (it == CloseableView.UNSTAGED_CHANGES_SEARCH) {
                    onSearchFilterToggledUnstaged(false)
                }
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
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        stageEntryUseCase(git, statusEntry)
    }

    fun unstage(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        unstageEntryUseCase(git, statusEntry)
    }


    fun unstageAll() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        taskType = TaskType.UNSTAGE_ALL_FILES,
    ) { git ->
        unstageAllUseCase(git)

        null
    }

    fun stageAll() = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        taskType = TaskType.STAGE_ALL_FILES,
    ) { git ->
        stageAllUseCase(git)

        null
    }

    fun resetStaged(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
    ) { git ->
        resetEntryUseCase(git, statusEntry, staged = true)
    }

    fun resetUnstaged(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
    ) { git ->
        resetEntryUseCase(git, statusEntry, staged = false)
    }

    private suspend fun loadStatus(git: Git) {
        val previousStatus = _stageState.value
        val type = if (
            git.repository.repositoryState.isRebasing ||
            git.repository.repositoryState.isMerging ||
            git.repository.repositoryState.isReverting ||
            git.repository.repositoryState.isCherryPicking
        ) {
            MessageType.MERGE
        } else {
            MessageType.NORMAL
        }

        if (type != savedCommitMessage.type) {
            savedCommitMessage = CommitMessage(messageByRepoState(git), type)
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
                    unstaged = unstaged,
                    isPartiallyReloading = false,
                )
            }
        } catch (ex: Exception) {
            _stageState.value = previousStatus
            throw ex
        }
    }

    private fun List<StatusEntry>.prioritizeConflicts(): List<StatusEntry> {
        return this.groupBy { it.filePath }
            .map {
                val statusEntries = it.value
                return@map if (statusEntries.count() == 1) {
                    statusEntries.first()
                } else {
                    val conflictingEntry =
                        statusEntries.firstOrNull { entry -> entry.statusType == StatusType.CONFLICTING }

                    conflictingEntry ?: statusEntries.first()
                }
            }
    }

    private fun messageByRepoState(git: Git): String {
        val message: String? =
            if (git.repository.repositoryState.isRebasing) {
                val rebaseMergeDir = File(git.repository.directory, "rebase-merge")
                val messageFile = File(rebaseMergeDir, "message")

                if (messageFile.exists()) {
                    runCatching { messageFile.readText() }.getOrNull() ?: ""
                } else {
                    ""
                }
            } else if (
                git.repository.repositoryState.isMerging ||
                git.repository.repositoryState.isReverting ||
                git.repository.repositoryState.isCherryPicking
            ) {
                git.repository.readMergeCommitMsg()
            } else {
                git.repository.readCommitEditMsg()
            }

        //TODO this replace is a workaround until this issue gets fixed https://github.com/JetBrains/compose-jb/issues/615
        return message.orEmpty().replace("\t", "    ")
    }

    private suspend fun loadHasUncommittedChanges(git: Git) = withContext(Dispatchers.IO) {
        lastUncommittedChangesState = checkHasUncommittedChangesUseCase(git)
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
        taskType = TaskType.DO_COMMIT,
    ) { git ->
        val amend = isAmend.value

        val commitMessage = if (amend && message.isBlank()) {
            getLastCommitMessageUseCase(git)
        } else
            message


        val personIdent = getPersonIdent(git)

        doCommitUseCase(git, commitMessage, amend, personIdent)

        updateCommitMessage("")
        _commitMessageChangesFlow.emit("")
        _isAmend.value = false

        positiveNotification(if (isAmend.value) "Commit amended" else "New commit created")
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
        loadHasUncommittedChanges(git)
    }

    fun continueRebase(message: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.CONTINUE_REBASE,
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

        null
    }

    fun abortRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.ABORT_REBASE,
    ) { git ->
        abortRebaseUseCase(git)

        positiveNotification("Rebase aborted")
    }

    fun skipRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.SKIP_REBASE,
    ) { git ->
        skipRebaseUseCase(git)

        null
    }

    fun resetRepoState() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.RESET_REPO_STATE,
    ) { git ->
        resetRepositoryStateUseCase(git)

        positiveNotification("Repository state has been reset")
    }

    fun deleteFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
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

    fun stagedTreeDirectoryClicked(directoryPath: String) {
        val contractedDirectories = treeContractedDirectories.value

        if (contractedDirectories.contains(directoryPath)) {
            treeContractedDirectories.value -= directoryPath
        } else {
            treeContractedDirectories.value += directoryPath
        }
    }

    fun alternateShowAsTree() {
        appSettingsRepository.showChangesAsTree = !appSettingsRepository.showChangesAsTree
    }

    fun stageByDirectory(dir: String) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        stageByDirectoryUseCase(git, dir)
    }

    fun unstageByDirectory(dir: String) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        unstageByDirectoryUseCase(git, dir)
    }

    fun addStagedSearchToCloseableView() {
        addSearchToCloseView(CloseableView.STAGED_CHANGES_SEARCH)
    }

    private fun removeStagedSearchToCloseableView() {
        removeSearchFromCloseView(CloseableView.STAGED_CHANGES_SEARCH)
    }

    fun addUnstagedSearchToCloseableView() {
        addSearchToCloseView(CloseableView.UNSTAGED_CHANGES_SEARCH)
    }

    private fun removeUnstagedSearchToCloseableView() {
        removeSearchFromCloseView(CloseableView.UNSTAGED_CHANGES_SEARCH)
    }

    private fun addSearchToCloseView(view: CloseableView) = tabScope.launch {
        tabState.addCloseableView(view)
    }

    private fun removeSearchFromCloseView(view: CloseableView) = tabScope.launch {
        tabState.removeCloseableView(view)
    }
}

sealed interface StageState {
    data object Loading : StageState
    data class Loaded(
        val staged: List<StatusEntry>,
        val unstaged: List<StatusEntry>,
        val isPartiallyReloading: Boolean,
    ) : StageState
}


sealed interface StageStateUi {
    val hasStagedFiles: Boolean
    val hasUnstagedFiles: Boolean
    val isLoading: Boolean
    val haveConflictsBeenSolved: Boolean

    data object Loading : StageStateUi {
        override val hasStagedFiles: Boolean
            get() = false
        override val hasUnstagedFiles: Boolean
            get() = false
        override val isLoading: Boolean
            get() = true
        override val haveConflictsBeenSolved: Boolean
            get() = false
    }

    sealed interface Loaded : StageStateUi

    data class TreeLoaded(
        val staged: List<TreeItem<StatusEntry>>,
        val unstaged: List<TreeItem<StatusEntry>>,
        val isPartiallyReloading: Boolean,
    ) : Loaded {

        override val hasStagedFiles: Boolean = staged.isNotEmpty()
        override val hasUnstagedFiles: Boolean = unstaged.isNotEmpty()
        override val isLoading: Boolean = isPartiallyReloading
        override val haveConflictsBeenSolved: Boolean = unstaged.none {
            it is TreeItem.File && it.data.statusType == StatusType.CONFLICTING
        }
    }

    data class ListLoaded(
        val staged: List<StatusEntry>,
        val unstaged: List<StatusEntry>,
        val isPartiallyReloading: Boolean,
    ) : Loaded {
        override val hasStagedFiles: Boolean = staged.isNotEmpty()
        override val hasUnstagedFiles: Boolean = unstaged.isNotEmpty()
        override val isLoading: Boolean = isPartiallyReloading
        override val haveConflictsBeenSolved: Boolean = unstaged.none { it.statusType == StatusType.CONFLICTING }
    }
}

data class CommitMessage(val message: String, val type: MessageType)

enum class MessageType {
    NORMAL,
    MERGE;
}

sealed interface CommitterDataRequestState {
    data object None : CommitterDataRequestState
    data class WaitingInput(val authorInfo: AuthorInfo) : CommitterDataRequestState
    data class Accepted(val authorInfo: AuthorInfo, val persist: Boolean) : CommitterDataRequestState
    object Reject : CommitterDataRequestState
}
