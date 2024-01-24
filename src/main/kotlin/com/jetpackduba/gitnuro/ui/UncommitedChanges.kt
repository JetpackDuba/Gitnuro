@file:OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.abortButton
import com.jetpackduba.gitnuro.theme.tertiarySurface
import com.jetpackduba.gitnuro.theme.textFieldColors
import com.jetpackduba.gitnuro.ui.components.*
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.EntryType
import com.jetpackduba.gitnuro.ui.context_menu.statusDirEntriesContextMenuItems
import com.jetpackduba.gitnuro.ui.context_menu.statusEntriesContextMenuItems
import com.jetpackduba.gitnuro.ui.dialogs.CommitAuthorDialog
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import com.jetpackduba.gitnuro.viewmodels.CommitterDataRequestState
import com.jetpackduba.gitnuro.viewmodels.StageStateUi
import com.jetpackduba.gitnuro.viewmodels.StatusViewModel
import org.eclipse.jgit.lib.RepositoryState

@Composable
fun UncommittedChanges(
    statusViewModel: StatusViewModel = gitnuroViewModel(),
    selectedEntryType: DiffEntryType?,
    repositoryState: RepositoryState,
    onStagedDiffEntrySelected: (StatusEntry?) -> Unit,
    onUnstagedDiffEntrySelected: (StatusEntry) -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
) {
    val stageStateUi = statusViewModel.stageStateUi.collectAsState().value
    val swapUncommittedChanges by statusViewModel.swapUncommittedChanges.collectAsState()
    val (commitMessage, setCommitMessage) = remember(statusViewModel) { mutableStateOf(statusViewModel.savedCommitMessage.message) }
    val stagedListState by statusViewModel.stagedLazyListState.collectAsState()
    val unstagedListState by statusViewModel.unstagedLazyListState.collectAsState()
    val isAmend by statusViewModel.isAmend.collectAsState()
    val isAmendRebaseInteractive by statusViewModel.isAmendRebaseInteractive.collectAsState()
    val committerDataRequestState = statusViewModel.committerDataRequestState.collectAsState()
    val committerDataRequestStateValue = committerDataRequestState.value
    val rebaseInteractiveState = statusViewModel.rebaseInteractiveState.collectAsState().value

    val showSearchStaged by statusViewModel.showSearchStaged.collectAsState()
    val searchFilterStaged by statusViewModel.searchFilterStaged.collectAsState()
    val showSearchUnstaged by statusViewModel.showSearchUnstaged.collectAsState()
    val searchFilterUnstaged by statusViewModel.searchFilterUnstaged.collectAsState()

    val isAmenableRebaseInteractive =
        repositoryState.isRebasing && rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits && rebaseInteractiveState.isCurrentStepAmenable

    val doCommit = {
        statusViewModel.commit(commitMessage)
        onStagedDiffEntrySelected(null)
        setCommitMessage("")
    }

    val canCommit = commitMessage.isNotEmpty() && stageStateUi.hasStagedFiles
    val canAmend = commitMessage.isNotEmpty() && statusViewModel.hasPreviousCommits

    LaunchedEffect(statusViewModel) {
        statusViewModel.commitMessageChangesFlow.collect { newCommitMessage ->
            setCommitMessage(newCommitMessage)
        }
    }

    if (committerDataRequestStateValue is CommitterDataRequestState.WaitingInput) {
        CommitAuthorDialog(
            committerDataRequestStateValue.authorInfo,
            onClose = { statusViewModel.onRejectCommitterData() },
            onAccept = { newAuthorInfo, persist ->
                statusViewModel.onAcceptCommitterData(newAuthorInfo, persist)
            },
        )
    }

    Column(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = stageStateUi.isLoading,
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
                    StagedView(
                        stageStateUi,
                        showSearchStaged,
                        searchFilterStaged,
                        stagedListState,
                        selectedEntryType,
                        onSearchFilterToggled = { statusViewModel.onSearchFilterToggledStaged(it) },
                        onDiffEntryOptionSelected = { statusViewModel.unstage(it) },
                        onDiffEntrySelected = onStagedDiffEntrySelected,
                        onSearchFilterChanged = { statusViewModel.onSearchFilterChangedStaged(it) },
                        onBlameFile = onBlameFile,
                        onHistoryFile = onHistoryFile,
                        onReset = { statusViewModel.resetStaged(it) },
                        onDelete = { statusViewModel.deleteFile(it) },
                        onAllAction = { statusViewModel.unstageAll() },
                        onAlternateShowAsTree = { statusViewModel.alternateShowAsTree() },
                        onTreeDirectoryClicked = { statusViewModel.stagedTreeDirectoryClicked(it) },
                        onTreeDirectoryAction = { statusViewModel.unstageByDirectory(it) },
                    )
                }

                @Composable
                fun unstaged() {
                    UnstagedView(
                        stageStateUi,
                        showSearchUnstaged,
                        searchFilterUnstaged,
                        unstagedListState,
                        selectedEntryType,
                        onSearchFilterToggled = { statusViewModel.onSearchFilterToggledUnstaged(it) },
                        onDiffEntryOptionSelected = { statusViewModel.stage(it) },
                        onDiffEntrySelected = onUnstagedDiffEntrySelected,
                        onSearchFilterChanged = { statusViewModel.onSearchFilterChangedUnstaged(it) },
                        onBlameFile = onBlameFile,
                        onHistoryFile = onHistoryFile,
                        onReset = { statusViewModel.resetUnstaged(it) },
                        onDelete = { statusViewModel.deleteFile(it) },
                        onAllAction = { statusViewModel.stageAll() },
                        onAlternateShowAsTree = { statusViewModel.alternateShowAsTree() },
                        onTreeDirectoryClicked = { statusViewModel.stagedTreeDirectoryClicked(it) },
                        onTreeDirectoryAction = { statusViewModel.stageByDirectory(it) },
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
            stageStateUi.haveConflictsBeenSolved,
            setCommitMessage = {
                setCommitMessage(it)
                statusViewModel.updateCommitMessage(it)
            },
            onResetRepoState = {
                statusViewModel.resetRepoState()
                statusViewModel.updateCommitMessage("")
            },
            onAbortRebase = {
                statusViewModel.abortRebase()
                statusViewModel.updateCommitMessage("")
            },
            onAmendChecked = { statusViewModel.amend(it) },
            onContinueRebase = { statusViewModel.continueRebase(commitMessage) },
            onSkipRebase = { statusViewModel.skipRebase() },
            onAmendRebaseInteractiveChecked = { statusViewModel.amendRebaseInteractive(it) }
        )
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
                    "Commit message (read-only)"
                } else {
                    "Write your commit message here"
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
fun ColumnScope.StagedView(
    stageStateUi: StageStateUi.Loaded,
    showSearchUnstaged: Boolean,
    searchFilterUnstaged: TextFieldValue,
    stagedListState: LazyListState,
    selectedEntryType: DiffEntryType?,
    onSearchFilterToggled: (Boolean) -> Unit,
    onDiffEntryOptionSelected: (StatusEntry) -> Unit,
    onDiffEntrySelected: (StatusEntry) -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
    onReset: (StatusEntry) -> Unit,
    onDelete: (StatusEntry) -> Unit,
    onAllAction: () -> Unit,
    onAlternateShowAsTree: () -> Unit,
    onTreeDirectoryClicked: (String) -> Unit,
    onTreeDirectoryAction: (String) -> Unit,
) {
    val title = "Staged"
    val actionTitle = "Untage"
    val allActionTitle = "Unstage all"
    val actionColor = MaterialTheme.colors.error
    val actionTextColor = MaterialTheme.colors.onError
    val actionIcon = AppIcons.REMOVE_DONE

    this.NeutralView(
        title = title,
        actionTitle = actionTitle,
        allActionTitle = allActionTitle,
        actionColor = actionColor,
        actionTextColor = actionTextColor,
        actionIcon = actionIcon,
        entryType = EntryType.STAGED,
        stageStateUi = stageStateUi,
        showSearchUnstaged = showSearchUnstaged,
        searchFilterUnstaged = searchFilterUnstaged,
        listState = stagedListState,
        selectedEntryType = selectedEntryType,
        onSearchFilterToggled = onSearchFilterToggled,
        onDiffEntryOptionSelected = onDiffEntryOptionSelected,
        onDiffEntrySelected = onDiffEntrySelected,
        onSearchFilterChanged = onSearchFilterChanged,
        onBlameFile = onBlameFile,
        onHistoryFile = onHistoryFile,
        onReset = onReset,
        onDelete = onDelete,
        onAllAction = onAllAction,
        onAlternateShowAsTree = onAlternateShowAsTree,
        onTreeDirectoryClicked = onTreeDirectoryClicked,
        onTreeDirectoryAction = onTreeDirectoryAction,
        onTreeEntries = { it.staged },
        onListEntries = { it.staged },
        onGetSelectedEntry = { if (selectedEntryType is DiffEntryType.StagedDiff) selectedEntryType else null }
    )
}

@Composable
fun ColumnScope.UnstagedView(
    stageStateUi: StageStateUi.Loaded,
    showSearchUnstaged: Boolean,
    searchFilterUnstaged: TextFieldValue,
    unstagedListState: LazyListState,
    selectedEntryType: DiffEntryType?,
    onSearchFilterToggled: (Boolean) -> Unit,
    onDiffEntryOptionSelected: (StatusEntry) -> Unit,
    onDiffEntrySelected: (StatusEntry) -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
    onReset: (StatusEntry) -> Unit,
    onDelete: (StatusEntry) -> Unit,
    onAllAction: () -> Unit,
    onAlternateShowAsTree: () -> Unit,
    onTreeDirectoryClicked: (String) -> Unit,
    onTreeDirectoryAction: (String) -> Unit,
) {
    val title = "Unstaged"
    val actionTitle = "Stage"
    val allActionTitle = "Stage all"
    val actionColor = MaterialTheme.colors.primary
    val actionTextColor = MaterialTheme.colors.onPrimary
    val actionIcon = AppIcons.DONE

    this.NeutralView(
        title = title,
        actionTitle = actionTitle,
        allActionTitle = allActionTitle,
        actionColor = actionColor,
        actionTextColor = actionTextColor,
        actionIcon = actionIcon,
        entryType = EntryType.UNSTAGED,
        stageStateUi = stageStateUi,
        showSearchUnstaged = showSearchUnstaged,
        searchFilterUnstaged = searchFilterUnstaged,
        listState = unstagedListState,
        selectedEntryType = selectedEntryType,
        onSearchFilterToggled = onSearchFilterToggled,
        onDiffEntryOptionSelected = onDiffEntryOptionSelected,
        onDiffEntrySelected = onDiffEntrySelected,
        onSearchFilterChanged = onSearchFilterChanged,
        onBlameFile = onBlameFile,
        onHistoryFile = onHistoryFile,
        onReset = onReset,
        onDelete = onDelete,
        onAllAction = onAllAction,
        onAlternateShowAsTree = onAlternateShowAsTree,
        onTreeDirectoryClicked = onTreeDirectoryClicked,
        onTreeDirectoryAction = onTreeDirectoryAction,
        onTreeEntries = { it.unstaged },
        onListEntries = { it.unstaged },
        onGetSelectedEntry = { if (selectedEntryType is DiffEntryType.UnstagedDiff) selectedEntryType else null }
    )
}

@Composable
fun ColumnScope.NeutralView(
    title: String,
    actionTitle: String,
    allActionTitle: String,
    actionColor: Color,
    actionTextColor: Color,
    actionIcon: String,
    entryType: EntryType,
    stageStateUi: StageStateUi.Loaded,
    showSearchUnstaged: Boolean,
    searchFilterUnstaged: TextFieldValue,
    listState: LazyListState,
    selectedEntryType: DiffEntryType?,
    onTreeEntries: (StageStateUi.TreeLoaded) -> List<TreeItem<StatusEntry>>,
    onListEntries: (StageStateUi.ListLoaded) -> List<StatusEntry>,
    onSearchFilterToggled: (Boolean) -> Unit,
    onDiffEntryOptionSelected: (StatusEntry) -> Unit,
    onDiffEntrySelected: (StatusEntry) -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
    onReset: (StatusEntry) -> Unit,
    onDelete: (StatusEntry) -> Unit,
    onAllAction: () -> Unit,
    onAlternateShowAsTree: () -> Unit,
    onTreeDirectoryClicked: (String) -> Unit,
    onTreeDirectoryAction: (String) -> Unit,
    onGetSelectedEntry: () -> DiffEntryType?,
) {
    val modifier = Modifier
        .weight(5f)
        .padding(bottom = 4.dp)
        .fillMaxWidth()

    if (stageStateUi is StageStateUi.TreeLoaded) {
        TreeEntriesList(
            modifier = modifier,
            title = title,
            actionTitle = actionTitle,
            actionColor = actionColor,
            actionTextColor = actionTextColor,
            actionIcon = actionIcon,
            showSearch = showSearchUnstaged,
            searchFilter = searchFilterUnstaged,
            onSearchFilterToggled = onSearchFilterToggled,
            onSearchFilterChanged = onSearchFilterChanged,
            statusEntries = onTreeEntries(stageStateUi),
            lazyListState = listState,
            onDiffEntrySelected = onDiffEntrySelected,
            onDiffEntryOptionSelected = onDiffEntryOptionSelected,
            onGenerateContextMenu = { statusEntry ->
                statusEntriesContextMenuItems(
                    statusEntry = statusEntry,
                    entryType = entryType,
                    onBlame = { onBlameFile(statusEntry.filePath) },
                    onHistory = { onHistoryFile(statusEntry.filePath) },
                    onReset = { onReset(statusEntry) },
                    onDelete = { onDelete(statusEntry) },
                )
            },
            onAllAction = onAllAction,
            onTreeDirectoryClicked = { onTreeDirectoryClicked(it.fullPath) },
            allActionTitle = allActionTitle,
            selectedEntryType = if (selectedEntryType is DiffEntryType.UnstagedDiff) selectedEntryType else null,
            onAlternateShowAsTree = onAlternateShowAsTree,
            onGenerateDirectoryContextMenu = { dir ->
                statusDirEntriesContextMenuItems(
                    entryType = entryType,
                    onStageChanges = { onTreeDirectoryAction(dir.fullPath) },
                    onDiscardDirectoryChanges = {},
                )
            }
        )
    } else if (stageStateUi is StageStateUi.ListLoaded) {
        EntriesList(
            modifier = modifier,
            title = title,
            actionTitle = actionTitle,
            actionColor = actionColor,
            actionTextColor = actionTextColor,
            actionIcon = actionIcon,
            showSearch = showSearchUnstaged,
            searchFilter = searchFilterUnstaged,
            onSearchFilterToggled = onSearchFilterToggled,
            onSearchFilterChanged = onSearchFilterChanged,
            statusEntries = onListEntries(stageStateUi),
            lazyListState = listState,
            onDiffEntrySelected = onDiffEntrySelected,
            onDiffEntryOptionSelected = onDiffEntryOptionSelected,
            onGenerateContextMenu = { statusEntry ->
                statusEntriesContextMenuItems(
                    statusEntry = statusEntry,
                    entryType = entryType,
                    onBlame = { onBlameFile(statusEntry.filePath) },
                    onHistory = { onHistoryFile(statusEntry.filePath) },
                    onReset = { onReset(statusEntry) },
                    onDelete = { onDelete(statusEntry) },
                )
            },
            onAllAction = onAllAction,
            allActionTitle = allActionTitle,
            selectedEntryType = onGetSelectedEntry(),
            onAlternateShowAsTree = onAlternateShowAsTree,
        )
    }
}

@Composable
fun UncommittedChangesButtons(
    canCommit: Boolean,
    canAmend: Boolean,
    isAmend: Boolean,
    onAmendChecked: (Boolean) -> Unit,
    onCommit: () -> Unit
) {
    val buttonText = if (isAmend)
        "Amend"
    else
        "Commit"

    Column {
        CheckboxText(
            value = isAmend,
            onCheckedChange = { onAmendChecked(!isAmend) },
            text = "Amend previous commit"
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
            text = "Merge",
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
            text = "Commit",
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
                text = "Amend previous commit"
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
                    text = "Continue",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .fillMaxHeight(),
                    enabled = haveConflictsBeenSolved,
                    onClick = onContinue,
                )
            } else {
                ConfirmationButton(
                    text = "Skip",
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
            text = "Continue",
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
            .clickable { onClick() }
            .focusable(false)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colors.abortButton),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Abort",
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
            .clickable { if (enabled) onClick() }
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
private fun EntriesList(
    modifier: Modifier,
    title: String,
    actionTitle: String,
    actionColor: Color,
    actionTextColor: Color,
    actionIcon: String,
    showSearch: Boolean,
    searchFilter: TextFieldValue,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    statusEntries: List<StatusEntry>,
    lazyListState: LazyListState,
    onDiffEntrySelected: (StatusEntry) -> Unit,
    onDiffEntryOptionSelected: (StatusEntry) -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
    onAllAction: () -> Unit,
    onAlternateShowAsTree: () -> Unit,
    allActionTitle: String,
    selectedEntryType: DiffEntryType?,
) {
    Column(
        modifier = modifier
    ) {
        EntriesHeader(
            title = title,
            actionColor = actionColor,
            allActionTitle = allActionTitle,
            actionTextColor = actionTextColor,
            actionIcon = actionIcon,
            onAllAction = onAllAction,
            onAlternateShowAsTree = onAlternateShowAsTree,
            searchFilter = searchFilter,
            onSearchFilterChanged = onSearchFilterChanged,
            onSearchFilterToggled = onSearchFilterToggled,
            showAsTree = false,
            showSearch = showSearch,
        )


        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            state = lazyListState,
        ) {
            items(statusEntries, key = { it.filePath }) { statusEntry ->
                val isEntrySelected = selectedEntryType != null &&
                        selectedEntryType is DiffEntryType.UncommittedDiff && // Added for smartcast
                        selectedEntryType.statusEntry == statusEntry
                UncommittedFileEntry(
                    statusEntry = statusEntry,
                    isSelected = isEntrySelected,
                    actionTitle = actionTitle,
                    actionColor = actionColor,
                    showDirectory = true,
                    onClick = {
                        onDiffEntrySelected(statusEntry)
                    },
                    onButtonClick = {
                        onDiffEntryOptionSelected(statusEntry)
                    },
                    onGenerateContextMenu = onGenerateContextMenu,
                )
            }
        }
    }
}

@Composable
private fun TreeEntriesList(
    modifier: Modifier,
    title: String,
    actionTitle: String,
    actionColor: Color,
    actionTextColor: Color,
    actionIcon: String,
    showSearch: Boolean,
    searchFilter: TextFieldValue,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    statusEntries: List<TreeItem<StatusEntry>>,
    lazyListState: LazyListState,
    onDiffEntrySelected: (StatusEntry) -> Unit,
    onDiffEntryOptionSelected: (StatusEntry) -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
    onGenerateDirectoryContextMenu: (TreeItem.Dir) -> List<ContextMenuElement>,
    onAllAction: () -> Unit,
    onAlternateShowAsTree: () -> Unit,
    onTreeDirectoryClicked: (TreeItem.Dir) -> Unit,
    allActionTitle: String,
    selectedEntryType: DiffEntryType?,
) {
    Column(
        modifier = modifier
    ) {
        EntriesHeader(
            title = title,
            actionColor = actionColor,
            allActionTitle = allActionTitle,
            actionTextColor = actionTextColor,
            actionIcon = actionIcon,
            onAllAction = onAllAction,
            onAlternateShowAsTree = onAlternateShowAsTree,
            searchFilter = searchFilter,
            onSearchFilterChanged = onSearchFilterChanged,
            onSearchFilterToggled = onSearchFilterToggled,
            showAsTree = true,
            showSearch = showSearch,
        )

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            state = lazyListState,
        ) {
            items(statusEntries, key = { it.fullPath }) { treeEntry ->
                val isEntrySelected = treeEntry is TreeItem.File<StatusEntry> &&
                        selectedEntryType != null &&
                        selectedEntryType is DiffEntryType.UncommittedDiff && // Added for smartcast
                        selectedEntryType.statusEntry == treeEntry.data

                UncommittedTreeItemEntry(
                    treeEntry,
                    isSelected = isEntrySelected,
                    actionTitle = actionTitle,
                    actionColor = actionColor,
                    onClick = {
                        if (treeEntry is TreeItem.File<StatusEntry>) {
                            onDiffEntrySelected(treeEntry.data)
                        } else if (treeEntry is TreeItem.Dir) {
                            onTreeDirectoryClicked(treeEntry)
                        }
                    },
                    onButtonClick = {
                        if (treeEntry is TreeItem.File<StatusEntry>) {
                            onDiffEntryOptionSelected(treeEntry.data)
                        }
                    },
                    onGenerateContextMenu = onGenerateContextMenu,
                    onGenerateDirectoryContextMenu = onGenerateDirectoryContextMenu,
                )
            }
        }
    }
}

@Composable
fun EntriesHeader(
    title: String,
    showAsTree: Boolean,
    showSearch: Boolean,
    allActionTitle: String,
    actionIcon: String,
    actionColor: Color,
    actionTextColor: Color,
    onAllAction: () -> Unit,
    onAlternateShowAsTree: () -> Unit,
    onSearchFilterToggled: (Boolean) -> Unit,
    searchFilter: TextFieldValue,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
) {

    val searchFocusRequester = remember { FocusRequester() }

    /**
     * State used to prevent the text field from getting the focus when returning from another tab
     */
    var requestFocus by remember { mutableStateOf(false) }

    val headerHoverInteraction = remember { MutableInteractionSource() }
    val isHeaderHovered by headerHoverInteraction.collectIsHoveredAsState()
    Column {
        Row(
            modifier = Modifier
                .height(34.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.tertiarySurface)
                .hoverable(headerHoverInteraction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .weight(1f),
                text = title,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            IconButton(
                onClick = {
                    onAlternateShowAsTree()
                },
                modifier = Modifier.handOnHover()
            ) {
                Icon(
                    painter = painterResource(if (showAsTree) AppIcons.LIST else AppIcons.TREE),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.onBackground,
                )
            }

            IconButton(
                onClick = {
                    onSearchFilterToggled(!showSearch)

                    if (!showSearch)
                        requestFocus = true
                },
                modifier = Modifier.handOnHover()
            ) {
                Icon(
                    painter = painterResource(AppIcons.SEARCH),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.onBackground,
                )
            }

            SecondaryButtonCompactable(
                text = allActionTitle,
                icon = actionIcon,
                isParentHovered = isHeaderHovered,
                backgroundButton = actionColor,
                onBackgroundColor = actionTextColor,
                onClick = onAllAction,
                modifier = Modifier.padding(start = 4.dp, end = 16.dp),
            )
        }



        if (showSearch) {
            SearchTextField(
                searchFilter = searchFilter,
                onSearchFilterChanged = onSearchFilterChanged,
                searchFocusRequester = searchFocusRequester,
                onClose = { onSearchFilterToggled(false) },
            )
        }

        LaunchedEffect(showSearch, requestFocus) {
            if (showSearch && requestFocus) {
                searchFocusRequester.requestFocus()
                requestFocus = false
            }
        }

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
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
) {
    UncommittedFileEntry(
        statusEntry = fileEntry.data,
        isSelected = isSelected,
        showDirectory = false,
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
            onClick,
            onButtonClick,
            onGenerateContextMenu,
        )

        is TreeItem.Dir -> DirectoryEntry(
            entry.displayName,
            onClick,
            depth = entry.depth,
            onGenerateContextMenu = { onGenerateDirectoryContextMenu(entry) },
        )
    }
}

//@Composable
//private fun TreeItemEntry(
//    entry: TreeItem<StatusEntry>,
//    isSelected: Boolean,
//    actionTitle: String,
//    actionColor: Color,
//    onClick: () -> Unit,
//    onButtonClick: () -> Unit,
//    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
//    onGenerateDirectoryContextMenu: (TreeItem.Dir) -> List<ContextMenuElement>,
//) {
//    when (entry) {
//        is TreeItem.File -> TreeFileEntry(
//            entry,
//            isSelected,
//            actionTitle,
//            actionColor,
//            onClick,
//            onButtonClick,
//            onGenerateContextMenu,
//        )
//
//        is TreeItem.Dir -> TreeDirEntry(
//            entry,
//            onClick,
//            onGenerateDirectoryContextMenu,
//        )
//    }
//}

internal fun placeRightOrBottom(
    totalSize: Int,
    size: IntArray,
    outPosition: IntArray,
    reverseInput: Boolean
) {
    val consumedSize = size.fold(0) { a, b -> a + b }
    var current = totalSize - consumedSize
    size.forEachIndexed(reverseInput) { index, it ->
        outPosition[index] = current
        current += it
    }
}

private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (Int, Int) -> Unit) {
    if (!reversed) {
        forEachIndexed(action)
    } else {
        for (i in (size - 1) downTo 0) {
            action(i, get(i))
        }
    }
}