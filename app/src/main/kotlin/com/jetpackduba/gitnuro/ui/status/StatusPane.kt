@file:OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.LocalTabFocusRequester
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.EntryType
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.extensions.fileName
import com.jetpackduba.gitnuro.domain.extensions.isCherryPicking
import com.jetpackduba.gitnuro.domain.extensions.isMerging
import com.jetpackduba.gitnuro.domain.extensions.isReverting
import com.jetpackduba.gitnuro.domain.extensions.parentDirectoryPath
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.abortButton
import com.jetpackduba.gitnuro.theme.textFieldColors
import com.jetpackduba.gitnuro.ui.components.*
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.statusDirEntriesContextMenuItems
import com.jetpackduba.gitnuro.ui.context_menu.statusEntriesContextMenuItems
import com.jetpackduba.gitnuro.ui.context_menu.statusEntryContextMenuItems
import com.jetpackduba.gitnuro.ui.dialogs.CommitAuthorDialog
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.RepositoryState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatusPane(
    statusPaneViewModel: StatusPaneViewModel,
    repositoryState: RepositoryState,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
) {
    val stageStateUi = statusPaneViewModel.stageStateUi.collectAsState().value
    val swapUncommittedChanges by statusPaneViewModel.swapUncommittedChanges.collectAsState(false)
    val (commitMessage, setCommitMessage) = remember(statusPaneViewModel) { mutableStateOf(statusPaneViewModel.savedCommitMessage.message) }
    val stagedListState by statusPaneViewModel.stagedLazyListState.collectAsState()
    val unstagedListState by statusPaneViewModel.unstagedLazyListState.collectAsState()
    val isAmend by statusPaneViewModel.isAmend.collectAsState()
    val isAmendRebaseInteractive by statusPaneViewModel.isAmendRebaseInteractive.collectAsState()
    val committerDataRequestState = statusPaneViewModel.committerDataRequestState.collectAsState()
    val committerDataRequestStateValue = committerDataRequestState.value
    val rebaseInteractiveState = statusPaneViewModel.rebaseInteractiveState.collectAsState().value
    val selectedUnstagedDiffEntries by statusPaneViewModel.selectedUnstagedDiffEntries.collectAsState()
    val selectedStagedDiffEntries by statusPaneViewModel.selectedStagedDiffEntries.collectAsState()

    val showSearchStaged by statusPaneViewModel.showSearchStaged.collectAsState()
    val showAsTree by statusPaneViewModel.showAsTree.collectAsState(false)
    val searchFilterStaged by statusPaneViewModel.searchFilterStaged.collectAsState()
    val showSearchUnstaged by statusPaneViewModel.showSearchUnstaged.collectAsState()
    val searchFilterUnstaged by statusPaneViewModel.searchFilterUnstaged.collectAsState()

    val isAmenableRebaseInteractive =
        repositoryState.isRebasing && rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits && rebaseInteractiveState.isCurrentStepAmenable

    val doCommit = {
        statusPaneViewModel.commit(commitMessage)
        Unit
    }

    val canCommit = commitMessage.isNotEmpty() && stageStateUi.hasStagedFiles
    val canAmend = commitMessage.isNotEmpty() && statusPaneViewModel.hasPreviousCommits
    val tabFocusRequester = LocalTabFocusRequester.current

    LaunchedEffect(statusPaneViewModel) {
        launch {
            statusPaneViewModel.commitMessageChangesFlow.collect { newCommitMessage ->
                setCommitMessage(newCommitMessage)
            }
        }

        launch {
            statusPaneViewModel.showSearchUnstaged.collectLatest { show ->
                if (!show) {
                    tabFocusRequester.requestFocus()
                }
            }
        }

        launch {
            statusPaneViewModel.showSearchStaged.collectLatest { show ->
                if (!show) {
                    tabFocusRequester.requestFocus()
                }
            }
        }
    }

    if (committerDataRequestStateValue is CommitterDataRequestState.WaitingInput) {
        CommitAuthorDialog(
            committerDataRequestStateValue.authorInfo,
            onClose = { statusPaneViewModel.onRejectCommitterData() },
            onAccept = { newAuthorInfo, persist ->
                statusPaneViewModel.onAcceptCommitterData(newAuthorInfo, persist)
            },
        )
    }

    Column(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = stageStateUi is StageStateUi.Loading || (stageStateUi is StageStateUi.Loaded && stageStateUi.isPartiallyReloading),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.primaryVariant)
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            if (stageStateUi is StageStateUi.Loaded) {
                @Composable
                fun staged() {
                    StatusChangesList(
                        entryType = EntryType.STAGED,
                        stageStateUi,
                        showSearchStaged,
                        showAsTree = showAsTree,
                        searchFilter = searchFilterStaged,
                        listState = stagedListState,
                        selectedEntries = selectedStagedDiffEntries,
                        onSearchFilterToggled = { statusPaneViewModel.onSearchFilterToggledStaged(it) },
                        onSearchFocused = { statusPaneViewModel.addStagedSearchToCloseableView() },
                        onBlameFile = onBlameFile,
                        onHistoryFile = onHistoryFile,
                        onAction = { statusPaneViewModel.onAction(it) },
                    )
                }

                @Composable
                fun unstaged() {
                    StatusChangesList(
                        entryType = EntryType.UNSTAGED,
                        stageStateUi = stageStateUi,
                        showSearch = showSearchUnstaged,
                        showAsTree = showAsTree,
                        searchFilter = searchFilterUnstaged,
                        listState = unstagedListState,
                        selectedEntries = selectedUnstagedDiffEntries,
                        onSearchFilterToggled = { statusPaneViewModel.onSearchFilterToggledUnstaged(it) },
                        onSearchFocused = { statusPaneViewModel.addUnstagedSearchToCloseableView() },
                        onBlameFile = onBlameFile,
                        onHistoryFile = onHistoryFile,
                        onAction = { statusPaneViewModel.onAction(it) },
                    )
                }

                if (swapUncommittedChanges) {
                    unstaged()
                    staged()
                } else {
                    staged()
                    unstaged()
                }
            }
        }

        CommitField(
            canCommit,
            isAmend,
            canAmend,
            doCommit,
            commitMessage,
            repositoryState,
            isAmenableRebaseInteractive,
            stageStateUi.hasUnstagedFiles,
            rebaseInteractiveState,
            stageStateUi.hasStagedFiles,
            isAmendRebaseInteractive,
            stageStateUi is StageStateUi.Loaded && stageStateUi.haveConflictsBeenSolved,
            setCommitMessage = {
                setCommitMessage(it)
                statusPaneViewModel.updateCommitMessage(it)
            },
            onResetRepoState = {
                statusPaneViewModel.resetRepoState()
                statusPaneViewModel.updateCommitMessage("")
            },
            onAbortRebase = {
                statusPaneViewModel.abortRebase()
                statusPaneViewModel.updateCommitMessage("")
            },
            onAmendChecked = { statusPaneViewModel.amend(it) },
            onContinueRebase = { statusPaneViewModel.continueRebase(commitMessage) },
            onSkipRebase = { statusPaneViewModel.skipRebase() },
            onAmendRebaseInteractiveChecked = { statusPaneViewModel.amendRebaseInteractive(it) }
        )
    }
}

@Composable
fun ColumnScope.StatusChangesList(
    entryType: EntryType,
    stageStateUi: StageStateUi.Loaded,
    showSearch: Boolean,
    showAsTree: Boolean,
    searchFilter: TextFieldValue,
    listState: LazyListState,
    selectedEntries: List<DiffType.UncommittedDiff>,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
    onAction: (StatusPaneAction) -> Unit,
) {
    val actionTitle = when (entryType) {
        EntryType.STAGED -> stringResource(Res.string.uncommited_changes_staged_title)
        EntryType.UNSTAGED -> stringResource(Res.string.uncommited_changes_unstaged_title)
    }

    val actionInfo = getActionInfo(entryType)
    val entries = if (searchFilter.text.trim().isEmpty()) {
        when (entryType) {
            EntryType.STAGED -> stageStateUi.staged
            EntryType.UNSTAGED -> stageStateUi.unstaged
        }
    } else {
        when (entryType) {
            EntryType.STAGED -> stageStateUi.filteredStaged
            EntryType.UNSTAGED -> stageStateUi.filteredUnstaged
        }
    }

    this.ChangesList(
        title = actionTitle,
        actionTitle = actionTitle,
        actionInfo = actionInfo,
        entryType = entryType,
        entries = entries,
        showSearch = showSearch,
        showAsTree = showAsTree,
        searchFilter = searchFilter,
        listState = listState,
        selectedEntries = selectedEntries,
        onSearchFilterToggled = onSearchFilterToggled,
        onSearchFocused = onSearchFocused,
        onBlameFile = onBlameFile,
        onHistoryFile = onHistoryFile,
        onAction = onAction,
    )
}

@Composable
fun ColumnScope.ChangesList(
    title: String,
    actionTitle: String,
    actionInfo: ActionInfo,
    entryType: EntryType,
    showSearch: Boolean,
    searchFilter: TextFieldValue,
    listState: LazyListState,
    selectedEntries: List<DiffType.UncommittedDiff>,
    showAsTree: Boolean,
    entries: List<TreeItem<StatusEntry>>,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
    onAction: (StatusPaneAction) -> Unit,
) {
    fun entriesContextMenu(): (StatusEntry) -> List<ContextMenuElement> = { statusEntry ->
        statusEntryContextMenuItems(
            statusEntry = statusEntry,
            entryType = entryType,
            onBlame = { onBlameFile(statusEntry.filePath) },
            onHistory = { onHistoryFile(statusEntry.filePath) },
            onReset = { onAction(StatusPaneAction.Reset(statusEntry)) },
            onDelete = { onAction(StatusPaneAction.Delete(statusEntry)) },
            onOpenFileInFolder = { onAction(StatusPaneAction.OpenInFolder(statusEntry.parentDirectoryPath)) },
            onCopyFilePath = { relative ->
                onAction(
                    StatusPaneAction.CopyPath(
                        relative = relative,
                        entries = listOf(statusEntry),
                    )
                )
            },
        )
    }

    fun selectedEntriesContextMenu(): (StatusEntry) -> List<ContextMenuElement> = {
        statusEntriesContextMenuItems(
            selectedEntriesCount = selectedEntries.count(),
            entryType = entryType,
            onDiscard = { onAction(StatusPaneAction.DiscardSelected(entryType)) },
            onStageSelected = { onAction(StatusPaneAction.SelectedEntriesAction(EntryType.UNSTAGED)) },
            onUnstageSelected = { onAction(StatusPaneAction.SelectedEntriesAction(EntryType.STAGED)) },
            onCopyFilesPath = { relative ->
                onAction(
                    StatusPaneAction.CopyPath(
                        relative = relative,
                        entries = selectedEntries.map { it.statusEntry },
                    )
                )
            },
        )
    }

    val showActionForSelected = remember(selectedEntries) { selectedEntries.count() > 1 }
    val keyboardModifiers = LocalWindowInfo.current.keyboardModifiers

    ChangesList(
        title = title,
        actionInfo = actionInfo,
        showSearch = showSearch,
        showAsTree = showAsTree,
        showActionForSelected = showActionForSelected,
        searchFilter = searchFilter,
        onSearchFilterToggled = onSearchFilterToggled,
        onSearchFocused = onSearchFocused,
        onSearchFilterChanged = { onAction(StatusPaneAction.SearchFilterChanged(it, entryType)) },
        listState = listState,
        onAllAction = { onAction(StatusPaneAction.AllEntriesAction(entryType)) },
        onAlternateShowAsTree = { onAction(StatusPaneAction.ToggleShowAsTree) },
    ) {
        items(entries, key = { it.fullPath }) { treeEntry ->
            val isEntrySelected = treeEntry is TreeItem.File<StatusEntry> &&
                    selectedEntries.any { entry ->
                        entry.statusEntry == treeEntry.data && ((entry.isUnstagedDiff && entryType == EntryType.UNSTAGED) ||
                                (entry.isStagedDiff && entryType == EntryType.STAGED))
                    }

            UncommittedTreeItemEntry(
                treeEntry,
                isSelected = isEntrySelected,
                actionTitle = actionTitle,
                actionColor = actionInfo.color,
                showAsTree = showAsTree,
                onClick = {
                    if (treeEntry is TreeItem.File<StatusEntry>) {
                        onAction(
                            StatusPaneAction.SelectEntry(
                                statusEntry = treeEntry.data,
                                isCtrlPressed = keyboardModifiers.isCtrlPressed,
                                isMetaPressed = keyboardModifiers.isMetaPressed,
                                isShiftPressed = keyboardModifiers.isShiftPressed,
                                diffEntries = entries,
                                selectedEntries = selectedEntries,
                            )
                        )
                    } else if (treeEntry is TreeItem.Dir) {
                        onAction(
                            StatusPaneAction.TreeDirectoryToggle(
                                treeEntry.fullPath
                            )
                        )
                    }
                },
                onButtonClick = {
                    if (treeEntry is TreeItem.File<StatusEntry>) {
                        onAction(StatusPaneAction.EntryAction(treeEntry.data))
                    }
                },
                onGenerateContextMenu = if (isEntrySelected && selectedEntries.count() > 1)
                    selectedEntriesContextMenu()
                else
                    entriesContextMenu(),
                onGenerateDirectoryContextMenu = { dir ->
                    statusDirEntriesContextMenuItems(
                        entryType = entryType,
                        onStageChanges = { onAction(StatusPaneAction.DirectoryAction(dir.fullPath, entryType)) },
                        onDiscardDirectoryChanges = {},
                    )
                },
            )
        }
    }
}

@Composable
fun ColumnScope.ChangesList(
    title: String,
    actionInfo: ActionInfo,
    showSearch: Boolean,
    showAsTree: Boolean,
    showActionForSelected: Boolean,
    searchFilter: TextFieldValue,
    listState: LazyListState,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    onAllAction: () -> Unit,
    onAlternateShowAsTree: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val modifier = Modifier
        .weight(5f)
        .padding(bottom = 4.dp)
        .fillMaxWidth()
    Column(
        modifier = modifier
    ) {
        FilesChangedHeader(
            title = title,
            actionInfo = actionInfo,
            onAllAction = onAllAction,
            onAlternateShowAsTree = onAlternateShowAsTree,
            searchFilter = searchFilter,
            onSearchFilterChanged = onSearchFilterChanged,
            onSearchFilterToggled = onSearchFilterToggled,
            onSearchFocused = onSearchFocused,
            showAsTree = showAsTree,
            showSearch = showSearch,
            showActionForSelected = showActionForSelected,
        )

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            state = listState,
        ) {
            this.content()
        }
    }
}


@Composable
private fun CommitField(
    canCommit: Boolean,
    isAmend: Boolean,
    canAmend: Boolean,
    doCommit: () -> Unit,
    commitMessage: String,
    repositoryState: RepositoryState,
    isAmenableRebaseInteractive: Boolean,
    hasUnstagedFiles: Boolean,
    rebaseInteractiveState: RebaseInteractiveState,
    hasStagedFiles: Boolean,
    isAmendRebaseInteractive: Boolean,
    haveConflictsBeenSolved: Boolean,
    setCommitMessage: (String) -> Unit,
    onResetRepoState: () -> Unit,
    onAbortRebase: () -> Unit,
    onContinueRebase: () -> Unit,
    onSkipRebase: () -> Unit,
    onAmendChecked: (Boolean) -> Unit,
    onAmendRebaseInteractiveChecked: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .height(192.dp)
            .fillMaxWidth()
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = true)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.TEXT_ACCEPT) && (canCommit || isAmend && canAmend)) {
                        doCommit()
                        true
                    } else
                        false
                },
            value = commitMessage,
            onValueChange = setCommitMessage,
            enabled = !repositoryState.isRebasing || isAmenableRebaseInteractive,
            label = {
                val text = if (repositoryState.isRebasing && !isAmenableRebaseInteractive) {
                    stringResource(Res.string.uncommited_changes_text_input_label_message_read_only)
                } else {
                    stringResource(Res.string.uncommited_changes_text_input_label_message)
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.primaryVariant,
                )
            },
            colors = textFieldColors(),
            textStyle = MaterialTheme.typography.body1,
        )

        when {
            repositoryState.isMerging -> MergeButtons(
                haveConflictsBeenSolved = !hasUnstagedFiles,
                onAbort = onResetRepoState,
                onMerge = { doCommit() }
            )

            repositoryState.isRebasing && rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits -> RebasingButtons(
                canContinue = hasStagedFiles || hasUnstagedFiles || (isAmenableRebaseInteractive && isAmendRebaseInteractive && commitMessage.isNotEmpty()),
                haveConflictsBeenSolved = !hasUnstagedFiles,
                onAbort = onAbortRebase,
                onContinue = onContinueRebase,
                onSkip = onSkipRebase,
                isAmendable = rebaseInteractiveState.isCurrentStepAmenable,
                isAmend = isAmendRebaseInteractive,
                onAmendChecked = onAmendRebaseInteractiveChecked,
            )

            repositoryState.isCherryPicking -> CherryPickingButtons(
                haveConflictsBeenSolved = !hasUnstagedFiles,
                onAbort = onResetRepoState,
                onCommit = {
                    doCommit()
                }
            )

            repositoryState.isReverting -> RevertingButtons(
                haveConflictsBeenSolved = haveConflictsBeenSolved,
                canCommit = commitMessage.isNotBlank(),
                onAbort = onResetRepoState,
                onCommit = {
                    doCommit()
                }
            )

            else -> UncommittedChangesButtons(
                canCommit = canCommit,
                canAmend = canAmend,
                isAmend = isAmend,
                onAmendChecked = onAmendChecked,
                onCommit = doCommit,
            )
        }
    }
}

@Composable
fun UncommittedChangesButtons(
    canCommit: Boolean,
    canAmend: Boolean,
    isAmend: Boolean,
    onAmendChecked: (Boolean) -> Unit,
    onCommit: () -> Unit,
) {
    val buttonText = if (isAmend)
        stringResource(Res.string.uncommited_changes_primary_button_amend)
    else
        stringResource(Res.string.uncommited_changes_primary_button_commit)

    Column {
        CheckboxText(
            value = isAmend,
            onCheckedChange = { onAmendChecked(!isAmend) },
            text = stringResource(Res.string.uncommited_changes_amend_check)
        )
        Row(
            modifier = Modifier
                .padding(top = 2.dp)
        ) {
            ConfirmationButton(
                text = buttonText,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                onClick = {
                    onCommit()
                },
                enabled = canCommit || (canAmend && isAmend),
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

@Composable
fun MergeButtons(
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onMerge: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .fillMaxHeight(),
            onClick = onAbort
        )

        ConfirmationButton(
            text = stringResource(Res.string.uncommited_changes_primary_button_merge),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = haveConflictsBeenSolved,
            onClick = onMerge,
        )
    }
}

@Composable
fun CherryPickingButtons(
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onCommit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .fillMaxHeight(),
            onClick = onAbort
        )

        ConfirmationButton(
            text = stringResource(Res.string.uncommited_changes_primary_button_commit),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = haveConflictsBeenSolved,
            onClick = onCommit,
        )
    }
}

@Composable
fun RebasingButtons(
    canContinue: Boolean,
    isAmendable: Boolean,
    isAmend: Boolean,
    onAmendChecked: (Boolean) -> Unit,
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column {
        if (isAmendable) {
            CheckboxText(
                value = isAmend,
                onCheckedChange = { onAmendChecked(!isAmend) },
                text = stringResource(Res.string.uncommited_changes_amend_check)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(36.dp)
        ) {
            AbortButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .fillMaxHeight(),
                onClick = onAbort
            )

            if (canContinue) {
                ConfirmationButton(
                    text = stringResource(Res.string.uncommited_changes_primary_button_continue),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .fillMaxHeight(),
                    enabled = haveConflictsBeenSolved,
                    onClick = onContinue,
                )
            } else {
                ConfirmationButton(
                    text = stringResource(Res.string.uncommited_changes_primary_button_skip),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .fillMaxHeight(),
                    onClick = onSkip,
                )
            }
        }
    }
}

@Composable
fun RevertingButtons(
    canCommit: Boolean,
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onCommit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            onClick = onAbort
        )

        ConfirmationButton(
            text = stringResource(Res.string.uncommited_changes_primary_button_continue),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = canCommit && haveConflictsBeenSolved,
            onClick = onCommit,
        )
    }
}

@Composable
fun AbortButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .handMouseClickable { onClick() }
            .focusable(false)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colors.abortButton),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.uncommited_changes_secondary_button_abort),
            style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onError),
        )
    }
}

@Composable
fun ConfirmationButton(
    text: String,
    modifier: Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    onClick: () -> Unit,
) {
    val (backgroundColor, contentColor) = if (enabled) {
        (MaterialTheme.colors.primary to MaterialTheme.colors.onPrimary)
    } else {
        (MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            .compositeOver(MaterialTheme.colors.surface) to MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled))
    }

    Box(
        modifier = modifier
            .handMouseClickable { if (enabled) onClick() }
            .focusable(false) // TODO this and the abort button should be focusable (show some kind of border when focused?)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body1.copy(color = contentColor),
        )
    }
}

@Composable
private fun UncommittedFileEntry(
    statusEntry: StatusEntry,
    isSelected: Boolean,
    showDirectory: Boolean,
    actionTitle: String,
    actionColor: Color,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    depth: Int = 0,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
) {
    FileEntry(
        icon = statusEntry.icon,
        depth = depth,
        iconColor = statusEntry.iconColor,
        parentDirectoryPath = if (showDirectory) statusEntry.parentDirectoryPath else "",
        fileName = statusEntry.fileName,
        isSelected = isSelected,
        onClick = onClick,
        onDoubleClick = onButtonClick,
        onGenerateContextMenu = { onGenerateContextMenu(statusEntry) },
        trailingAction = { isHovered ->
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                visible = isHovered,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SecondaryButton(
                    onClick = onButtonClick,
                    text = actionTitle,
                    backgroundButton = actionColor,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                )
            }
        }
    )
}

@Composable
private fun TreeFileEntry(
    fileEntry: TreeItem.File<StatusEntry>,
    isSelected: Boolean,
    actionTitle: String,
    actionColor: Color,
    showAsTree: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
) {
    UncommittedFileEntry(
        statusEntry = fileEntry.data,
        isSelected = isSelected,
        showDirectory = !showAsTree,
        actionTitle = actionTitle,
        actionColor = actionColor,
        onClick = onClick,
        onButtonClick = onDoubleClick,
        depth = fileEntry.depth,
        onGenerateContextMenu = onGenerateContextMenu,
    )
}

@Composable
private fun UncommittedTreeItemEntry(
    entry: TreeItem<StatusEntry>,
    isSelected: Boolean,
    actionTitle: String,
    actionColor: Color,
    showAsTree: Boolean,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
    onGenerateDirectoryContextMenu: (TreeItem.Dir) -> List<ContextMenuElement>,
) {
    when (entry) {
        is TreeItem.File -> TreeFileEntry(
            entry,
            isSelected,
            actionTitle,
            actionColor,
            showAsTree,
            onClick,
            onButtonClick,
            onGenerateContextMenu,
        )

        is TreeItem.Dir -> DirectoryEntry(
            dirName = entry.displayName,
            isExpanded = entry.isExpanded,
            onClick = onClick,
            depth = entry.depth,
            onGenerateContextMenu = { onGenerateDirectoryContextMenu(entry) },
        )
    }
}

@Composable
fun getActionInfo(entryType: EntryType): ActionInfo {
    val applyToOneTitle: String
    val applyToAllTitle: String
    val applyToSelectedTitle: String
    val icon: DrawableResource
    val color: Color
    val textColor: Color

    if (entryType == EntryType.STAGED) {
        applyToOneTitle = stringResource(Res.string.uncommited_changes_staged_item_action)
        applyToAllTitle = stringResource(Res.string.uncommited_changes_staged_all_items_action)
        applyToSelectedTitle = stringResource(Res.string.uncommited_changes_staged_selected_items_action)
        icon = Res.drawable.remove_done
        color = MaterialTheme.colors.error
        textColor = MaterialTheme.colors.onError
    } else {
        applyToOneTitle = stringResource(Res.string.uncommited_changes_unstaged_item_action)
        applyToAllTitle = stringResource(Res.string.uncommited_changes_unstaged_all_items_action)
        applyToSelectedTitle = stringResource(Res.string.uncommited_changes_unstaged_selected_items_action)
        icon = Res.drawable.done
        color = MaterialTheme.colors.primary
        textColor = MaterialTheme.colors.onPrimary
    }

    return ActionInfo(
        applyToOneTitle = applyToOneTitle,
        applyToAllTitle = applyToAllTitle,
        applyToSelectedTitle = applyToSelectedTitle,
        icon = icon,
        color = color,
        textColor = textColor,
    )
}
