package com.jetpackduba.gitnuro.ui.status

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.extensions.nullIf
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.data.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.data.repositories.DiffSelected
import com.jetpackduba.gitnuro.data.repositories.SelectedDiffItemRepository
import com.jetpackduba.gitnuro.domain.extensions.*
import com.jetpackduba.gitnuro.domain.git.DiffType
import com.jetpackduba.gitnuro.domain.git.EntryType
import com.jetpackduba.gitnuro.domain.git.author.LoadAuthorGitAction
import com.jetpackduba.gitnuro.domain.git.author.SaveAuthorGitAction
import com.jetpackduba.gitnuro.domain.git.log.GetLastCommitMessageGitAction
import com.jetpackduba.gitnuro.domain.git.log.GetSpecificCommitMessageGitAction
import com.jetpackduba.gitnuro.domain.git.rebase.AbortRebaseGitAction
import com.jetpackduba.gitnuro.domain.git.rebase.ContinueRebaseGitAction
import com.jetpackduba.gitnuro.domain.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.git.rebase.SkipRebaseGitAction
import com.jetpackduba.gitnuro.domain.git.repository.ResetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.git.workspace.*
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.StatusStageAllUseCase
import com.jetpackduba.gitnuro.domain.usecases.StatusStageUseCase
import com.jetpackduba.gitnuro.domain.usecases.StatusUnstageAllUseCase
import com.jetpackduba.gitnuro.domain.usecases.StatusUnstageUseCase
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import com.jetpackduba.gitnuro.ui.tree_files.entriesToTreeEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryState
import org.jetbrains.skiko.ClipboardManager
import java.io.File
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

private const val MIN_TIME_IN_MS_TO_SHOW_LOAD = 500L

class StatusPaneViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val unstageUseCase: StatusUnstageUseCase,
    private val stageUseCase: StatusStageUseCase,
    private val stageAllUseCase: StatusStageAllUseCase,
    private val unstageAllUseCase: StatusUnstageAllUseCase,
    private val stageByDirectoryGitAction: StageByDirectoryGitAction,
    private val unstageByDirectoryGitAction: UnstageByDirectoryGitAction,
    private val discardEntriesGitAction: DiscardEntriesGitAction,
    private val stageAllGitAction: StageAllGitAction,
    private val unstageAllGitAction: UnstageAllGitAction,
    private val getLastCommitMessageGitAction: GetLastCommitMessageGitAction,
    private val resetRepositoryStateGitAction: ResetRepositoryStateGitAction,
    private val continueRebaseGitAction: ContinueRebaseGitAction,
    private val abortRebaseGitAction: AbortRebaseGitAction,
    private val skipRebaseGitAction: SkipRebaseGitAction,
    private val getStatusGitAction: GetStatusGitAction,
    private val checkHasUncommittedChangesGitAction: CheckHasUncommittedChangesGitAction,
    private val doCommitGitAction: DoCommitGitAction,
    private val loadAuthorGitAction: LoadAuthorGitAction,
    private val saveAuthorGitAction: SaveAuthorGitAction,
    private val sharedRepositoryStateManager: SharedRepositoryStateManager,
    private val getSpecificCommitMessageGitAction: GetSpecificCommitMessageGitAction,
    private val appSettingsRepository: AppSettingsRepository,
    private val tabScope: CoroutineScope,
    private val selectedDiffItemRepository: SelectedDiffItemRepository,
    private val clipboardManager: ClipboardManager,
) {
    private val _showSearchUnstaged = MutableStateFlow(false)
    val showSearchUnstaged: StateFlow<Boolean> = _showSearchUnstaged

    private val _showSearchStaged = MutableStateFlow(false)
    val showSearchStaged: StateFlow<Boolean> = _showSearchStaged

    private val _searchFilterUnstaged = MutableStateFlow(TextFieldValue(""))
    val searchFilterUnstaged: StateFlow<TextFieldValue> = _searchFilterUnstaged

    private val _searchFilterStaged = MutableStateFlow(TextFieldValue(""))
    val searchFilterStaged: StateFlow<TextFieldValue> = _searchFilterStaged

    val selectedStagedDiffEntries = selectedDiffItemRepository
        .diffSelected
        .map { diffSelected ->
            getDiffSelectedEntriesByEntryType(diffSelected, EntryType.STAGED)
        }
        .stateIn(
            tabScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val selectedUnstagedDiffEntries = selectedDiffItemRepository
        .diffSelected
        .map { diffSelected ->
            getDiffSelectedEntriesByEntryType(diffSelected, EntryType.UNSTAGED)
        }
        .stateIn(
            tabScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    val swapUncommittedChanges = appSettingsRepository.swapUncommittedChangesFlow
    val rebaseInteractiveState = sharedRepositoryStateManager.rebaseInteractiveState

    private val treeContractedDirectories = MutableStateFlow(emptyList<String>())
    val showAsTree = appSettingsRepository.showChangesAsTreeFlow
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

            state.copy(filteredStaged = staged, filteredUnstaged = unstaged)

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
                StageStateUi.Loaded(
                    staged = entriesToTreeEntry(
                        showAsTree,
                        stageStateFiltered.staged,
                        contractedDirectories
                    ) { it.filePath },
                    unstaged = entriesToTreeEntry(
                        showAsTree,
                        stageStateFiltered.unstaged,
                        contractedDirectories
                    ) { it.filePath },
                    filteredStaged = entriesToTreeEntry(
                        showAsTree,
                        stageStateFiltered.filteredStaged,
                        contractedDirectories
                    ) { it.filePath },
                    filteredUnstaged = entriesToTreeEntry(
                        showAsTree,
                        stageStateFiltered.filteredUnstaged,
                        contractedDirectories
                    ) { it.filePath },
                    isPartiallyReloading = stageStateFiltered.isPartiallyReloading,
                )
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

        tabScope.launch {
            selectedDiffItemRepository
                .diffSelected
                .combine(_stageState) { diffSelected, state ->
                    diffSelected to state
                }
                .collectLatest { (diffSelected, state) ->
                    if (state is StageState.Loaded && diffSelected is DiffSelected.UncommittedChanges) {
                        val entries = state.getEntriesByEntryType(diffSelected.entryType)

                        val diffSelectedToRemove = diffSelected.items
                            .asSequence()
                            .filter { diff ->
                                entries.none { statusEntry ->
                                    statusEntry.filePath == diff.statusEntry.filePath &&
                                            statusEntry.entryType == diff.statusEntry.entryType
                                }
                            }
                            .toSet()

                        if (diffSelectedToRemove.isNotEmpty()) {
                            removeEntriesFromSelection(diffSelectedToRemove, diffSelected.entryType)
                        }
                    }
                }
        }
    }

    private fun removeEntriesFromSelection(
        diffSelectedToRemove: Set<DiffType.UncommittedDiff>,
        entryType: EntryType,
    ) {
        selectedDiffItemRepository.removeSelectedUncommited(
            selectedToRemove = diffSelectedToRemove,
            entryType = entryType,
        )
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

    private fun getDiffSelectedEntriesByEntryType(
        diffSelected: DiffSelected?,
        entryType: EntryType
    ): List<DiffType.UncommittedDiff> {
        val diffUncommited = diffSelected as? DiffSelected.UncommittedChanges

        return if (diffUncommited?.entryType == entryType) {
            diffUncommited.items
        } else {
            emptySet()
        }.toList()
    }

    fun onAction(action: StatusPaneAction) = when (action) {
        is StatusPaneAction.EntryAction -> {
            when (action.statusEntry.entryType) {
                EntryType.STAGED -> unstage(action.statusEntry)
                EntryType.UNSTAGED -> stage(action.statusEntry)
            }
        }

        is StatusPaneAction.Reset -> when (action.statusEntry.entryType) {
            EntryType.STAGED -> discardStaged(listOf(action.statusEntry))
            EntryType.UNSTAGED -> discardUnstaged(listOf(action.statusEntry))
        }

        is StatusPaneAction.AllEntriesAction -> when (action.entryType) {
            EntryType.STAGED -> unstageAll()
            EntryType.UNSTAGED -> stageAll()
        }

        is StatusPaneAction.Delete -> deleteFile(action.statusEntry)
        is StatusPaneAction.DirectoryAction -> when (action.entryType) {
            EntryType.STAGED -> unstageByDirectory(action.path)
            EntryType.UNSTAGED -> stageByDirectory(action.path)
        }

        is StatusPaneAction.OpenInFolder -> openFileInFolder(action.path)
        is StatusPaneAction.SearchFilterChanged -> when (action.entryType) {
            EntryType.STAGED -> onSearchFilterChangedStaged(action.filter)
            EntryType.UNSTAGED -> onSearchFilterChangedUnstaged(action.filter)
        }

        is StatusPaneAction.SelectEntry -> selectEntries(
            action.isCtrlPressed,
            action.isMetaPressed,
            action.isShiftPressed,
            diffEntries = action.diffEntries,
            selectedEntries = action.selectedEntries,
            entry = action.statusEntry
        )

        StatusPaneAction.ToggleShowAsTree -> alternateShowAsTree()
        is StatusPaneAction.TreeDirectoryToggle -> toggleTreeDirectoryVisibility(action.path)
        is StatusPaneAction.CopyPath -> copyEntriesPath(action.entries, action.relative)
        is StatusPaneAction.DiscardSelected -> when (action.entryType) {
            EntryType.STAGED -> discardSelectedStaged()
            EntryType.UNSTAGED -> discardSelectedUnstaged()
        }

        is StatusPaneAction.SelectedEntriesAction -> when (action.entryType) {
            EntryType.STAGED -> unstageAll()
            EntryType.UNSTAGED -> stageAll()
        }
    }

    private fun discardSelectedStaged() {
        discardStaged(selectedStagedDiffEntries.value.map { it.statusEntry })
    }

    private fun discardSelectedUnstaged() {
        discardUnstaged(selectedUnstagedDiffEntries.value.map { it.statusEntry })
    }

    private fun copyEntriesPath(
        entries: List<StatusEntry>,
        relative: Boolean
    ) = tabState.runOperation(refreshType = RefreshType.NONE) { git ->

        val repoAbsolutPath = git.repository.workTree.absolutePath
        val pathsToCopy = entries.joinToString("\n") { entry ->
            if (relative) {
                entry.filePath
            } else {
                repoAbsolutPath + systemSeparator + entry.filePath
            }
        }

        clipboardManager.setText(pathsToCopy)
    }

    private fun stage(statusEntry: StatusEntry) = stageUseCase(statusEntry)
    private fun unstage(statusEntry: StatusEntry) = unstageUseCase(statusEntry)

    private fun unstageAll() {
        val entries = selectedStagedDiffEntries
            .value
            .ifEmpty { null }
            ?.map { it.statusEntry }
            ?.nullIf { it.count() == 1 }

        unstageAllUseCase(entries)
    }

    private fun stageAll() {
        val entries = selectedUnstagedDiffEntries
            .value
            .ifEmpty { null }
            ?.map { it.statusEntry }
            ?.nullIf { it.count() == 1 }

        stageAllUseCase(entries)
    }

    private fun discardStaged(statusEntries: List<StatusEntry>) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
    ) { git ->
        discardEntriesGitAction(git, statusEntries, staged = true)
    }

    private fun discardUnstaged(statusEntries: List<StatusEntry>) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
    ) { git ->
        discardEntriesGitAction(git, statusEntries, staged = false)
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
                val status = getStatusGitAction(git.repository.directory.absolutePath)
                val staged = status.staged
                val unstaged = status.unstaged

                _stageState.value = StageState.Loaded(
                    staged = staged,
                    filteredStaged = staged,
                    unstaged = unstaged,
                    filteredUnstaged = unstaged,
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
        lastUncommittedChangesState = checkHasUncommittedChangesGitAction(git)
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
            getLastCommitMessageGitAction(git)
        } else
            message

        val personIdent = getPersonIdent(git)

        doCommitGitAction(git, commitMessage, amend, personIdent)

        updateCommitMessage("")
        _commitMessageChangesFlow.emit("")
        _isAmend.value = false

        positiveNotification(if (isAmend.value) "Commit amended" else "New commit created")
    }

    private suspend fun getPersonIdent(git: Git): PersonIdent? {
        val author = loadAuthorGitAction(git)

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
                    saveAuthorGitAction(git, authorInfo)
                }

                PersonIdent(authorInfo.globalName, authorInfo.globalEmail)
            } else {
                throw CancellationException("Author info request cancelled")
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
                doCommitGitAction(git, message, true, getPersonIdent(git))
            }
        }

        continueRebaseGitAction(git)

        null
    }

    fun abortRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.ABORT_REBASE,
    ) { git ->
        abortRebaseGitAction(git)

        positiveNotification("Rebase aborted")
    }

    fun skipRebase() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.SKIP_REBASE,
    ) { git ->
        skipRebaseGitAction(git)

        null
    }

    fun resetRepoState() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.RESET_REPO_STATE,
    ) { git ->
        resetRepositoryStateGitAction(git)

        positiveNotification("Repository state has been reset")
    }

    private fun deleteFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
    ) { git ->
        val path = statusEntry.filePath

        val fileToDelete = File(git.repository.workTree, path)

        fileToDelete.deleteRecursively()
    }

    fun openFileInFolder(folderPath: String?) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        if (folderPath != null) {
            val file = File(git.repository.workTree.absolutePath + File.separator + folderPath)
            file.openFileInFolder()
        }
    }

    fun updateCommitMessage(message: String) {
        savedCommitMessage = savedCommitMessage.copy(message = message)
        persistMessage()
    }

    private fun takeMessageFromPreviousCommit() = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        savedCommitMessage = savedCommitMessage.copy(message = getLastCommitMessageGitAction(git))
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
        val message = getSpecificCommitMessageGitAction(git, commitId)

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

    fun onSearchFilterToggledStaged(visible: Boolean? = null) {
        _showSearchStaged.value = visible ?: !_showSearchStaged.value
    }

    fun onSearchFilterChangedStaged(filter: TextFieldValue) {
        _searchFilterStaged.value = filter
    }

    fun onSearchFilterToggledUnstaged(visible: Boolean? = null) {
        _showSearchUnstaged.value = visible ?: !_showSearchUnstaged.value
    }

    fun onSearchFilterChangedUnstaged(filter: TextFieldValue) {
        _searchFilterUnstaged.value = filter
    }

    fun toggleTreeDirectoryVisibility(directoryPath: String) {
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
        stageByDirectoryGitAction(git, dir)
    }

    fun unstageByDirectory(dir: String) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        unstageByDirectoryGitAction(git, dir)
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

    fun selectEntries(
        isCtrlPressed: Boolean,
        isMetaPressed: Boolean,
        isShiftPressed: Boolean,
        diffEntries: List<TreeItem<StatusEntry>>,
        selectedEntries: List<DiffType.UncommittedDiff>,
        entry: StatusEntry,
    ) {
        val selectionType = getEntriesToSelect(
            isCtrlPressed = isCtrlPressed,
            isMetaPressed = isMetaPressed,
            isShiftPressed = isShiftPressed,
            diffEntries = diffEntries,
            selectedEntries = selectedEntries,
            entry = entry,
        )

        when (selectionType) {
            is SelectionType.AddMultipleEntries -> this.selectEntries(
                entry.entryType,
                selectionType.entries,
                addToExisting = true
            )

            is SelectionType.AppendSingleEntry -> this.selectEntries(
                entry.entryType,
                listOf(selectionType.entry),
                addToExisting = true,
            )

            is SelectionType.RemoveSingleEntry -> this.removeEntriesFromSelection(
                setOf(DiffType.UncommittedDiff(selectionType.entry, entry.entryType)),
                entry.entryType,
            )

            is SelectionType.SetSingleEntry -> this.selectEntries(
                entry.entryType,
                listOf(selectionType.entry),
                addToExisting = false,
            )
        }
    }

    private fun getEntriesToSelect(
        isCtrlPressed: Boolean,
        isMetaPressed: Boolean,
        isShiftPressed: Boolean,
        diffEntries: List<TreeItem<StatusEntry>>,
        selectedEntries: List<DiffType.UncommittedDiff>,
        entry: StatusEntry,
    ): SelectionType<StatusEntry> {
        return when {
            isShiftPressed -> {
                val entries =
                    getEntriesInBetween(
                        diffEntries,
                        selectedEntries,
                        entry,
                    )

                SelectionType.AddMultipleEntries(entries)
            }

            currentOs == OS.MAC && isMetaPressed || isCtrlPressed -> {
                val isAlreadyPresent = selectedEntries.any { it.statusEntry == entry }

                if (isAlreadyPresent) {
                    SelectionType.RemoveSingleEntry(entry)
                } else {
                    SelectionType.AppendSingleEntry(entry)
                }
            }

            else -> SelectionType.SetSingleEntry(entry)
        }
    }

    private fun getEntriesInBetween(
        diffEntries: List<TreeItem<StatusEntry>>,
        selectedEntries: List<DiffType>,
        entry: StatusEntry,
    ): List<StatusEntry> {
        val entries = diffEntries
            .filterIsInstance<TreeItem.File<StatusEntry>>()
            .map { it.data }

        val last = selectedEntries.lastOrNull()

        return if (last == null) {
            listOf(entry)
        } else {
            // Should always be uncommitted diff at this point
            val statusEntry = (last as DiffType.UncommittedDiff).statusEntry
            val lastItemIndex = entries.indexOf(statusEntry)
            val selectedItemIndex = entries.indexOf(entry)

            val entriesToSelect =
                entries.subList(min(lastItemIndex, selectedItemIndex), max(lastItemIndex, selectedItemIndex) + 1)

            entriesToSelect
        }
    }

    fun selectEntries(entryType: EntryType, entries: List<StatusEntry>, addToExisting: Boolean) {
        selectedDiffItemRepository.addDiffUncommited(
            entries.map {
                DiffType.UncommittedDiff(
                    statusEntry = it,
                    entryType = entryType,
                )
            },
            addToExisting,
            entryType,
        )
    }
}

sealed interface SelectionType<T> {
    data class SetSingleEntry<T>(val entry: T) : SelectionType<T>
    data class AppendSingleEntry<T>(val entry: T) : SelectionType<T>
    data class RemoveSingleEntry<T>(val entry: T) : SelectionType<T>
    data class AddMultipleEntries<T>(val entries: List<T>) : SelectionType<T>
}

sealed interface StageState {
    data object Loading : StageState
    data class Loaded(
        val staged: List<StatusEntry>,
        val filteredStaged: List<StatusEntry>,
        val unstaged: List<StatusEntry>,
        val filteredUnstaged: List<StatusEntry>,
        val isPartiallyReloading: Boolean,
    ) : StageState {
        fun getEntriesByEntryType(entryType: EntryType): List<StatusEntry> {
            return when (entryType) {
                EntryType.STAGED -> staged
                EntryType.UNSTAGED -> unstaged
            }
        }
    }
}


sealed interface StageStateUi {
    val hasStagedFiles: Boolean
        get() {
            return this is Loaded && staged.isNotEmpty()
        }

    val hasUnstagedFiles: Boolean
        get() {
            return this is Loaded && unstaged.isNotEmpty()
        }

    data object Loading : StageStateUi

    data class Loaded(
        val staged: List<TreeItem<StatusEntry>>,
        val filteredStaged: List<TreeItem<StatusEntry>>,
        val unstaged: List<TreeItem<StatusEntry>>,
        val filteredUnstaged: List<TreeItem<StatusEntry>>,
        val isPartiallyReloading: Boolean,
    ) : StageStateUi {
        val haveConflictsBeenSolved: Boolean = unstaged.none {
            it is TreeItem.File && it.data.statusType == StatusType.CONFLICTING
        }
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
