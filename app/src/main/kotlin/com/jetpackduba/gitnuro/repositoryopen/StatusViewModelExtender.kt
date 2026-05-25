package com.jetpackduba.gitnuro.repositoryopen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.collectLatestInCoroutineScope
import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.extensions.nullIf
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.extensions.isCherryPicking
import com.jetpackduba.gitnuro.domain.extensions.isMerging
import com.jetpackduba.gitnuro.domain.extensions.isReverting
import com.jetpackduba.gitnuro.domain.extensions.openFileInFolder
import com.jetpackduba.gitnuro.domain.interfaces.IGetLastCommitMessageGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetSpecificCommitMessageGitAction
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.repositories.*
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.domain.usecases.*
import com.jetpackduba.gitnuro.extensions.stateIn
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.ui.status.*
import com.jetpackduba.gitnuro.ui.status.MessageType
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

private const val PERSIST_MESSAGE_DELAY_IN_MS = 1_000L


class StatusViewModelExtender @AssistedInject constructor(
    val appStateManager: AppStateManager,
    private val tabState: TabInstanceRepository,
    private val setClipboardContentUseCase: SetClipboardContentUseCase,
    private val unstageUseCase: StatusUnstageUseCase,
    private val stageUseCase: StatusStageUseCase,
    private val stageAllUseCase: StatusStageAllUseCase,
    private val unstageAllUseCase: StatusUnstageAllUseCase,
    private val discardEntriesUseCase: DiscardEntriesUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val getLastCommitMessageGitAction: IGetLastCommitMessageGitAction,
    private val resetRepositoryStateUseCase: ResetRepositoryStateUseCase,
    private val abortRebaseUseCase: AbortRebaseUseCase,
    private val continueRebaseUseCase: ContinueRebaseUseCase,
    private val skipRebaseUseCase: SkipRebaseUseCase,
    private val doCommitUseCase: DoCommitUseCase,
    private val getAuthorUseCase: GetAuthorUseCase,
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val getSpecificCommitMessageGitAction: IGetSpecificCommitMessageGitAction,
    private val appSettings: AppSettingsService,
    private val addSelectedDiffUseCase: AddSelectedDiffUseCase,
    private val stageByDirectoryUseCase: StageByDirectoryUseCase,
    private val unstageByDirectoryUseCase: UnstageByDirectoryUseCase,
    private val persistCommitMessageUseCase: PersistCommitMessageUseCase,
    private val repositoryDataRepository: RepositoryDataRepository,
    @Assisted private val viewModelScope: CoroutineScope,
    @Assisted private val showAsTree: Flow<Boolean>,
    @Assisted private val diffSelected: StateFlow<DiffSelected?>,
    @Assisted private val rebaseInteractiveState: StateFlow<RebaseInteractiveState>,
    @Assisted private val onDiffSelected: (DiffSelected) -> Unit,
    @Assisted private val onRemoveEntriesFromSelection: (Set<DiffType.UncommittedDiff>, EntryType) -> Unit,
    @Assisted private val onAlternateShowAsTree: () -> Unit,
) : CoroutineScope by viewModelScope {

    @AssistedFactory
    interface Factory {
        fun create(
            viewModelScope: CoroutineScope,
            showAsTree: Flow<Boolean>,
            diffSelected: StateFlow<DiffSelected?>,
            rebaseInteractiveState: StateFlow<RebaseInteractiveState>,
            onDiffSelected: (DiffSelected) -> Unit,
            onRemoveEntriesFromSelection: (Set<DiffType.UncommittedDiff>, EntryType) -> Unit,
            onAlternateShowAsTree: () -> Unit,
        ): StatusViewModelExtender
    }

    val showSearchUnstaged: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val showSearchStaged: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val searchFilterUnstaged: StateFlow<TextFieldValue>
        field = MutableStateFlow(TextFieldValue(""))

    val searchFilterStaged: StateFlow<TextFieldValue>
        field = MutableStateFlow(TextFieldValue(""))

    private val treeContractedDirectories = MutableStateFlow(emptyList<String>())


    val swapUncommittedChanges = appSettings.swapStatusPanes

    var savedCommitMessage = CommitMessage("", MessageType.NORMAL)

    // When false, disable "amend previous commit"
    // TODO This should be improved in case it's a dangling branch, shouldn't happen often but could be a thing
    var hasPreviousCommits = repositoryDataRepository.log.map { it.isNotEmpty() }

    val stagedLazyListState = MutableStateFlow(LazyListState(0, 0))
    val unstagedLazyListState = MutableStateFlow(LazyListState(0, 0))

    val committerDataRequestState: StateFlow<CommitterDataRequestState>
        field = MutableStateFlow<CommitterDataRequestState>(CommitterDataRequestState.None)

    /**
     * Notify the UI that the commit message has been changed by the view model
     */
    val commitMessageChangesFlow: SharedFlow<String>
        field = MutableSharedFlow<String>()

    val isAmend: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private val _isAmendRebaseInteractive =
        MutableStateFlow(true) // TODO should copy message from previous commit when this is required
    val isAmendRebaseInteractive: StateFlow<Boolean> = _isAmendRebaseInteractive


    private var persistMessageJob: Job? = null

    val selectedStagedDiffEntries = diffSelected
        .map { diffSelected ->
            getDiffSelectedEntriesByEntryType(diffSelected, EntryType.STAGED)
        }
        .stateIn(emptyList())

    val selectedUnstagedDiffEntries = diffSelected
        .map { diffSelected ->
            getDiffSelectedEntriesByEntryType(diffSelected, EntryType.UNSTAGED)
        }
        .stateIn(emptyList())


    val statusState = combineStatusState(
        repositoryDataRepository.status,
        showSearchStaged,
        searchFilterStaged,
        showSearchUnstaged,
        searchFilterUnstaged,
        showAsTree,
        treeContractedDirectories,
        swapUncommittedChanges,
        stagedLazyListState,
        unstagedLazyListState,
        isAmend,
        isAmendRebaseInteractive,
        committerDataRequestState,
        rebaseInteractiveState,
        selectedUnstagedDiffEntries,
        selectedStagedDiffEntries,
        hasPreviousCommits,
        repositoryDataRepository.repositoryState,
    )
        .stateIn(StatusState())

    init {
        showSearchStaged.collectLatestInCoroutineScope {
            if (it) {
                addStagedSearchToCloseableView()
            } else {
                removeStagedSearchToCloseableView()
            }
        }

        showSearchUnstaged.collectLatestInCoroutineScope {
            if (it) {
                addUnstagedSearchToCloseableView()
            } else {
                removeUnstagedSearchToCloseableView()
            }
        }



        diffSelected
            .combine(statusState) { diffSelected, state ->
                diffSelected to state
            }
            .collectLatestInCoroutineScope { (diffSelected, state) ->
                if (diffSelected is DiffSelected.UncommittedChanges) {
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
                        onRemoveEntriesFromSelection(diffSelectedToRemove, diffSelected.entryType)
                    }
                }
            }

    }

    fun onAction(action: StatusAction) = when (action) {
        is StatusAction.EntryAction -> {
            when (action.statusEntry.entryType) {
                EntryType.STAGED -> unstage(action.statusEntry)
                EntryType.UNSTAGED -> stage(action.statusEntry)
            }
        }

        is StatusAction.Reset -> when (action.statusEntry.entryType) {
            EntryType.STAGED -> discardStaged(listOf(action.statusEntry))
            EntryType.UNSTAGED -> discardUnstaged(listOf(action.statusEntry))
        }

        is StatusAction.AllEntriesAction -> when (action.entryType) {
            EntryType.STAGED -> unstageAll()
            EntryType.UNSTAGED -> stageAll()
        }

        is StatusAction.Delete -> deleteFile(action.statusEntry)
        is StatusAction.DirectoryAction -> when (action.entryType) {
            EntryType.STAGED -> unstageByDirectory(action.path)
            EntryType.UNSTAGED -> stageByDirectory(action.path)
        }

        is StatusAction.OpenInFolder -> openFileInFolder(action.path)
        is StatusAction.SearchFilterChanged -> when (action.entryType) {
            EntryType.STAGED -> onSearchFilterChangedStaged(action.filter)
            EntryType.UNSTAGED -> onSearchFilterChangedUnstaged(action.filter)
        }

        is StatusAction.SelectEntry -> selectEntries(
            action.isCtrlPressed,
            action.isMetaPressed,
            action.isShiftPressed,
            diffEntries = action.diffEntries,
            selectedEntries = action.selectedEntries,
            entry = action.statusEntry
        )

        StatusAction.ToggleShowAsTree -> onAlternateShowAsTree()
        is StatusAction.TreeDirectoryToggle -> toggleTreeDirectoryVisibility(action.path)
        is StatusAction.CopyPath -> copyEntriesPath(action.entries, action.relative)
        is StatusAction.DiscardSelected -> when (action.entryType) {
            EntryType.STAGED -> discardSelectedStaged()
            EntryType.UNSTAGED -> discardSelectedUnstaged()
        }

        is StatusAction.SelectedEntriesAction -> when (action.entryType) {
            EntryType.STAGED -> unstageAll()
            EntryType.UNSTAGED -> stageAll()
        }

        StatusAction.AbortRebase -> abortRebase()
        is StatusAction.AcceptCommitterData -> acceptCommitterData(action.authorInfo, action.persist)
        StatusAction.RejectCommitterData -> rejectCommitterData()
        StatusAction.AddStagedSearchToCloseableView -> addStagedSearchToCloseableView()
        StatusAction.AddUnstagedSearchToCloseableView -> addUnstagedSearchToCloseableView()
        is StatusAction.Commit -> commit(action.message)
        is StatusAction.ContinueRebase -> continueRebase(action.message)
        StatusAction.ResetRepositoryState -> resetRepoState()
        is StatusAction.SearchFilterToggledStaged -> searchFilterToggledStaged()
        is StatusAction.SearchFilterToggledUnstaged -> searchFilterToggledUnstaged()
        StatusAction.SkipRebase -> skipRebase()
        is StatusAction.ToggleAmend -> amend(action.toggle)
        is StatusAction.ToggleAmendRebaseInteractive -> amendRebaseInteractive(action.toggle)
        is StatusAction.UpdateCommitMessage -> updateCommitMessage(action.message)
    }


    private fun copyEntriesPath(
        entries: List<StatusEntry>,
        relative: Boolean
    ) = viewModelScope.launch {
        val repoAbsolutPath = repositoryDataRepository.repositoryPath ?: return@launch
        val pathsToCopy = entries.joinToString("\n") { entry ->
            if (relative) {
                entry.filePath
            } else {
                repoAbsolutPath + systemSeparator + entry.filePath
            }
        }

        setClipboardContentUseCase(pathsToCopy)
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
        commitMessageChangesFlow.emit(savedCommitMessage.message)
    }

    fun rejectCommitterData() {
        this.committerDataRequestState.value = CommitterDataRequestState.Reject
    }

    fun acceptCommitterData(newAuthorInfo: AuthorInfo, persist: Boolean) {
        this.committerDataRequestState.value = CommitterDataRequestState.Accepted(newAuthorInfo, persist)
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
        commitMessageChangesFlow.emit(savedCommitMessage.message)
    }

    private fun persistMessage() {
        persistMessageJob?.cancel()

        persistMessageJob = viewModelScope.launch {
            delay(PERSIST_MESSAGE_DELAY_IN_MS.milliseconds)
            persistCommitMessageUseCase(savedCommitMessage.message.ifBlank { null })
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


    fun searchFilterToggledStaged(visible: Boolean? = null) {
        showSearchStaged.value = visible ?: !showSearchStaged.value
    }

    fun onSearchFilterChangedStaged(filter: TextFieldValue) {
        searchFilterStaged.value = filter
    }

    fun searchFilterToggledUnstaged(visible: Boolean? = null) {
        showSearchUnstaged.value = visible ?: !showSearchUnstaged.value
    }

    fun onSearchFilterChangedUnstaged(filter: TextFieldValue) {
        searchFilterUnstaged.value = filter
    }

    private fun discardSelectedStaged() {
        discardStaged(selectedStagedDiffEntries.value.map { it.statusEntry })
    }

    private fun discardSelectedUnstaged() {
        discardUnstaged(selectedUnstagedDiffEntries.value.map { it.statusEntry })
    }

    private fun stageByDirectory(dir: String) = stageByDirectoryUseCase(dir)

    private fun unstageByDirectory(dir: String) = unstageByDirectoryUseCase(dir)

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

            is SelectionType.RemoveSingleEntry -> this.onRemoveEntriesFromSelection(
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

    private fun selectEntries(entryType: EntryType, entries: List<StatusEntry>, addToExisting: Boolean) {
        val newValue = addSelectedDiffUseCase(
            diffSelected.value,
            entries.map {
                DiffType.UncommittedDiff(
                    statusEntry = it,
                    entryType = entryType,
                )
            },
            addToExisting,
            entryType,
        )

        onDiffSelected(newValue)
    }


    private fun stageAll() {
        val entries = selectedUnstagedDiffEntries
            .value
            .ifEmpty { null }
            ?.map { it.statusEntry }
            ?.nullIf { it.count() == 1 }

        stageAllUseCase(entries)
    }

    private fun discardStaged(statusEntries: List<StatusEntry>) {
        discardEntriesUseCase(statusEntries, isStaged = true)
    }

    private fun discardUnstaged(statusEntries: List<StatusEntry>) {
        discardEntriesUseCase(statusEntries, isStaged = false)
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

    private fun removeSearchFromCloseView(view: CloseableView) = viewModelScope.launch {
        tabState.removeCloseableView(view)
    }

    fun toggleTreeDirectoryVisibility(directoryPath: String) {
        val contractedDirectories = treeContractedDirectories.value

        if (contractedDirectories.contains(directoryPath)) {
            treeContractedDirectories.value -= directoryPath
        } else {
            treeContractedDirectories.value += directoryPath
        }
    }


    private fun addSearchToCloseView(view: CloseableView) = viewModelScope.launch {
        tabState.addCloseableView(view)
    }

    private fun continueRebase(message: String) = continueRebaseUseCase(message, isAmendRebaseInteractive.value)
    private fun abortRebase() = abortRebaseUseCase()
    private fun skipRebase() = skipRebaseUseCase()
    private fun resetRepoState() = resetRepositoryStateUseCase()

    private fun deleteFile(statusEntry: StatusEntry) = deleteFileUseCase(statusEntry.filePath)

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
            commitMessageChangesFlow.emit(savedCommitMessage.message)
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
        this.isAmend.value = isAmend

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

    fun commit(message: String) = viewModelScope.launch {
        val amend = isAmend.value

        val personIdent = getIdentity()

        doCommitUseCase(message, amend, personIdent)

        updateCommitMessage("")
        commitMessageChangesFlow.emit("")
        isAmend.value = false

        positiveNotification(if (isAmend.value) "Commit amended" else "New commit created")
    }

    private suspend fun getIdentity(): Identity? {
        val author = getAuthorUseCase().okOrNull() ?: return null

        return if (
            author.repositoryIdentity.name.isNullOrEmpty() && author.globalIdentity.name.isNullOrEmpty() ||
            author.repositoryIdentity.email.isNullOrEmpty() && author.globalIdentity.email.isNullOrEmpty()
        ) {
            committerDataRequestState.value = CommitterDataRequestState.WaitingInput(author)

            var committerData = committerDataRequestState.value

            while (committerData is CommitterDataRequestState.WaitingInput) {
                committerData = committerDataRequestState.value
            }

            if (committerData is CommitterDataRequestState.Accepted) {
                val authorInfo = committerData.authorInfo

                if (committerData.persist) {
                    saveAuthorUseCase(authorInfo)
                }

                Identity(authorInfo.globalIdentity.name.orEmpty(), authorInfo.globalIdentity.email.orEmpty())
            } else {
                throw CancellationException("Author info request cancelled")
            }
        } else
            null
    }

}