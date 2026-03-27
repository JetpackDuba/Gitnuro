package com.jetpackduba.gitnuro.ui.status

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.extensions.nullIf
import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.*
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.domain.usecases.*
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import com.jetpackduba.gitnuro.ui.tree_files.entriesToTreeEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git
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
    private val stageByDirectoryGitAction: IStageByDirectoryGitAction,
    private val unstageByDirectoryGitAction: IUnstageByDirectoryGitAction,
    private val discardEntriesGitAction: IDiscardEntriesGitAction,
    private val getLastCommitMessageGitAction: IGetLastCommitMessageGitAction,
    private val resetRepositoryStateGitAction: IResetRepositoryStateGitAction,
    private val continueRebaseGitAction: IContinueRebaseGitAction,
    private val abortRebaseGitAction: IAbortRebaseGitAction,
    private val skipRebaseGitAction: ISkipRebaseGitAction,
    private val doCommitUseCase: DoCommitUseCase,
    private val loadAuthorGitAction: ILoadAuthorGitAction,
    private val saveAuthorGitAction: ISaveAuthorGitAction,
    private val sharedRepositoryStateManager: SharedRepositoryStateManager,
    private val getSpecificCommitMessageGitAction: IGetSpecificCommitMessageGitAction,
    private val appSettings: AppSettingsService,
    private val tabScope: TabCoroutineScope,
    private val clipboardManager: ClipboardManager,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val removeSelectedDiffUseCase: RemoveSelectedDiffUseCase,
    private val addSelectedDiffUseCase: AddSelectedDiffUseCase,
) : TabViewModel() {
    private val _showSearchUnstaged = MutableStateFlow(false)
    val showSearchUnstaged: StateFlow<Boolean> = _showSearchUnstaged

    private val _showSearchStaged = MutableStateFlow(false)
    val showSearchStaged: StateFlow<Boolean> = _showSearchStaged

    private val _searchFilterUnstaged = MutableStateFlow(TextFieldValue(""))
    val searchFilterUnstaged: StateFlow<TextFieldValue> = _searchFilterUnstaged

    private val _searchFilterStaged = MutableStateFlow(TextFieldValue(""))
    val searchFilterStaged: StateFlow<TextFieldValue> = _searchFilterStaged

    val selectedStagedDiffEntries = repositoryDataRepository
        .diffSelected
        .map { diffSelected ->
            getDiffSelectedEntriesByEntryType(diffSelected, EntryType.STAGED)
        }
        .stateIn(
            tabScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val selectedUnstagedDiffEntries = repositoryDataRepository
        .diffSelected
        .map { diffSelected ->
            getDiffSelectedEntriesByEntryType(diffSelected, EntryType.UNSTAGED)
        }
        .stateIn(
            tabScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    private val treeContractedDirectories = MutableStateFlow(emptyList<String>())
    val showAsTree = appSettings.showChangesAsTree
    private val _statusState = MutableStateFlow<StatusState>(StatusState.Loading)

    val statusStateUi = combine(
        repositoryDataRepository.status,
        _showSearchStaged,
        _searchFilterStaged,
        _showSearchUnstaged,
        _searchFilterUnstaged,
        showAsTree,
        treeContractedDirectories,
    ) { status,
        showSearchStaged,
        filterStaged,
        showSearchUnstaged,
        filterUnstaged,
        showAsTree,
        contractedDirectories ->
        val filteredUnstaged = if (showSearchUnstaged && filterUnstaged.text.isNotBlank()) {
            status.unstaged.filter { it.filePath.lowercaseContains(filterUnstaged.text) }
        } else {
            status.unstaged
        }.prioritizeConflicts()

        val filteredStaged = if (showSearchStaged && filterStaged.text.isNotBlank()) {
            status.staged.filter { it.filePath.lowercaseContains(filterStaged.text) }
        } else {
            status.staged
        }.prioritizeConflicts()
        StatusStateUi.Loaded(
            statusEntriesToTreeEntry(
                showAsTree,
                status.staged,
                contractedDirectories
            ),
            statusEntriesToTreeEntry(
                showAsTree,
                filteredStaged,
                contractedDirectories
            ),
            statusEntriesToTreeEntry(
                showAsTree,
                status.unstaged,
                contractedDirectories
            ),
            statusEntriesToTreeEntry(
                showAsTree,
                filteredUnstaged,
                contractedDirectories
            ),
            false,
        )
    }
        .stateIn(
            tabScope,
            SharingStarted.Lazily,
            StatusStateUi.Loading
        )

    val swapUncommittedChanges = appSettings.swapStatusPanes
    val rebaseInteractiveState = sharedRepositoryStateManager.rebaseInteractiveState

    var savedCommitMessage = CommitMessage("", MessageType.NORMAL)

    var hasPreviousCommits = true // When false, disable "amend previous commit"

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
            repositoryDataRepository
                .diffSelected
                .combine(_statusState) { diffSelected, state ->
                    diffSelected to state
                }
                .collectLatest { (diffSelected, state) ->
                    if (state is StatusState.Loaded && diffSelected is DiffSelected.UncommittedChanges) {
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

    private fun statusEntriesToTreeEntry(
        showAsTree: Boolean,
        entries: List<StatusEntry>,
        contractedDirectories: List<String>
    ): List<TreeItem<StatusEntry>> {
        return entriesToTreeEntry(
            showAsTree,
            entries,
            contractedDirectories
        ) { it.filePath }
    }

    private fun removeEntriesFromSelection(
        diffSelectedToRemove: Set<DiffType.UncommittedDiff>,
        entryType: EntryType,
    ) {
        removeSelectedDiffUseCase(
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

    // TODO Load message somehow in the data+domain layer?
    private suspend fun loadStatus(git: Git) {
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

    fun commit(message: String) = tabScope.launch {
        val amend = isAmend.value

// TODO restore this val personIdent = getIdentity(git)
        val personIdent = null

        doCommitUseCase(message, amend, personIdent)

        updateCommitMessage("")
        _commitMessageChangesFlow.emit("")
        _isAmend.value = false

        positiveNotification(if (isAmend.value) "Commit amended" else "New commit created")
    }

    private suspend fun getIdentity(git: Git): Identity? {
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

                Identity(authorInfo.globalName.orEmpty(), authorInfo.globalEmail.orEmpty())
            } else {
                throw CancellationException("Author info request cancelled")
            }
        } else
            null
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
                doCommitUseCase(message, true, getIdentity(git))
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

    fun alternateShowAsTree() = tabScope.launch {
        appSettings.setConfiguration(AppConfig.ShowChangesAsTree(!appSettings.showChangesAsTree.first()))
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
        addSelectedDiffUseCase(
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

sealed interface StatusState {
    data object Loading : StatusState
    data class Loaded(
        val staged: List<StatusEntry>,
        val filteredStaged: List<StatusEntry>,
        val unstaged: List<StatusEntry>,
        val filteredUnstaged: List<StatusEntry>,
        val isPartiallyReloading: Boolean,
    ) : StatusState {
        fun getEntriesByEntryType(entryType: EntryType): List<StatusEntry> {
            return when (entryType) {
                EntryType.STAGED -> staged
                EntryType.UNSTAGED -> unstaged
            }
        }
    }
}


sealed interface StatusStateUi {
    val hasStagedFiles: Boolean
        get() {
            return this is Loaded && staged.isNotEmpty()
        }

    val hasUnstagedFiles: Boolean
        get() {
            return this is Loaded && unstaged.isNotEmpty()
        }

    data object Loading : StatusStateUi

    data class Loaded(
        val staged: List<TreeItem<StatusEntry>>,
        val filteredStaged: List<TreeItem<StatusEntry>>,
        val unstaged: List<TreeItem<StatusEntry>>,
        val filteredUnstaged: List<TreeItem<StatusEntry>>,
        val isPartiallyReloading: Boolean,
    ) : StatusStateUi {
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
