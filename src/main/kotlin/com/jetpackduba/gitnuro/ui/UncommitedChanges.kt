@file:OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.SecondaryButton
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.viewmodels.StageStatus
import com.jetpackduba.gitnuro.viewmodels.StatusViewModel
import org.eclipse.jgit.lib.RepositoryState

@Composable
fun UncommitedChanges(
    statusViewModel: StatusViewModel = gitnuroViewModel(),
    selectedEntryType: DiffEntryType?,
    repositoryState: RepositoryState,
    onStagedDiffEntrySelected: (StatusEntry?) -> Unit,
    onUnstagedDiffEntrySelected: (StatusEntry) -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
) {
    val stageStatusState = statusViewModel.stageStatus.collectAsState()
    var commitMessage by remember(statusViewModel) { mutableStateOf(statusViewModel.savedCommitMessage.message) }
    val stagedListState by statusViewModel.stagedLazyListState.collectAsState()
    val unstagedListState by statusViewModel.unstagedLazyListState.collectAsState()

    val stageStatus = stageStatusState.value
    val staged: List<StatusEntry>
    val unstaged: List<StatusEntry>
    val isLoading: Boolean

    if (stageStatus is StageStatus.Loaded) {
        staged = stageStatus.staged
        unstaged = stageStatus.unstaged
        isLoading = stageStatus.isPartiallyReloading
    } else {
        staged = listOf()
        unstaged = listOf() // return empty lists if still loading
        isLoading = true
    }

    val doCommit = { amend: Boolean ->
        statusViewModel.commit(commitMessage, amend)
        onStagedDiffEntrySelected(null)
        commitMessage = ""
    }

    val canCommit = commitMessage.isNotEmpty() && staged.isNotEmpty()
    val canAmend = (commitMessage.isNotEmpty() || staged.isNotEmpty()) && statusViewModel.hasPreviousCommits

    LaunchedEffect(Unit) {
        statusViewModel.commitMessageChangesFlow.collect { newCommitMessage ->
            commitMessage = newCommitMessage
        }
    }

    Column(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.primaryVariant)
        }

        EntriesList(
            modifier = Modifier
                .weight(5f)
                .padding(bottom = 4.dp)
                .fillMaxWidth(),
            title = "Staged",
            allActionTitle = "Unstage all",
            actionTitle = "Unstage",
            selectedEntryType = if (selectedEntryType is DiffEntryType.StagedDiff) selectedEntryType else null,
            actionColor = MaterialTheme.colors.error,
            actionTextColor = MaterialTheme.colors.onError,
            statusEntries = staged,
            lazyListState = stagedListState,
            onDiffEntrySelected = onStagedDiffEntrySelected,
            onDiffEntryOptionSelected = {
                statusViewModel.unstage(it)
            },
            onGenerateContextMenu = { statusEntry ->
                statusEntriesContextMenuItems(
                    statusEntry = statusEntry,
                    entryType = EntryType.STAGED,
                    onBlame = { onBlameFile(statusEntry.filePath) },
                    onReset = { statusViewModel.resetStaged(statusEntry) },
                    onHistory = { onHistoryFile(statusEntry.filePath) },
                )
            },
            onAllAction = {
                statusViewModel.unstageAll()
            },
        )

        EntriesList(
            modifier = Modifier
                .weight(5f)
                .padding(bottom = 4.dp)
                .fillMaxWidth(),
            title = "Unstaged",
            actionTitle = "Stage",
            selectedEntryType = if (selectedEntryType is DiffEntryType.UnstagedDiff) selectedEntryType else null,
            actionColor = MaterialTheme.colors.primary,
            actionTextColor = MaterialTheme.colors.onPrimary,
            statusEntries = unstaged,
            lazyListState = unstagedListState,
            onDiffEntrySelected = onUnstagedDiffEntrySelected,
            onDiffEntryOptionSelected = {
                statusViewModel.stage(it)
            },
            onGenerateContextMenu = { statusEntry ->
                statusEntriesContextMenuItems(
                    statusEntry = statusEntry,
                    entryType = EntryType.UNSTAGED,
                    onBlame = { onBlameFile(statusEntry.filePath) },
                    onHistory = { onHistoryFile(statusEntry.filePath) },
                    onReset = { statusViewModel.resetUnstaged(statusEntry) },
                    onDelete = {
                        statusViewModel.deleteFile(statusEntry)
                    },
                )
            },
            onAllAction = {
                statusViewModel.stageAll()
            },
            allActionTitle = "Stage all",
        )

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
                        if (keyEvent.matchesBinding(KeybindingOption.TEXT_ACCEPT) && canCommit) {
                            doCommit(false)
                            true
                        } else
                            false
                    },
                value = commitMessage,
                onValueChange = {
                    commitMessage = it

                    statusViewModel.updateCommitMessage(it)
                },
                enabled = !repositoryState.isRebasing,
                label = {
                    val text = if (repositoryState.isRebasing) {
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
                    haveConflictsBeenSolved = unstaged.isEmpty(),
                    onAbort = {
                        statusViewModel.resetRepoState()
                        statusViewModel.updateCommitMessage("")
                    },
                    onMerge = { doCommit(false) }
                )

                repositoryState.isRebasing -> RebasingButtons(
                    canContinue = staged.isNotEmpty() || unstaged.isNotEmpty(),
                    haveConflictsBeenSolved = unstaged.isEmpty(),
                    onAbort = {
                        statusViewModel.abortRebase()
                        statusViewModel.updateCommitMessage("")
                    },
                    onContinue = { statusViewModel.continueRebase() },
                    onSkip = { statusViewModel.skipRebase() },
                )

                repositoryState.isCherryPicking -> CherryPickingButtons(
                    haveConflictsBeenSolved = unstaged.isEmpty(),
                    onAbort = {
                        statusViewModel.resetRepoState()
                        statusViewModel.updateCommitMessage("")
                    },
                    onCommit = {
                        doCommit(false)
                    }
                )

                repositoryState.isReverting -> RevertingButtons(
                    haveConflictsBeenSolved = unstaged.none { it.statusType == StatusType.CONFLICTING },
                    canCommit = commitMessage.isNotBlank(),
                    onAbort = {
                        statusViewModel.resetRepoState()
                        statusViewModel.updateCommitMessage("")
                    },
                    onCommit = {
                        doCommit(false)
                    }
                )

                else -> UncommitedChangesButtons(
                    canCommit = canCommit,
                    canAmend = canAmend,
                    onCommit = { amend -> doCommit(amend) },
                )
            }
        }
    }

}

@Composable
fun UncommitedChangesButtons(
    canCommit: Boolean,
    canAmend: Boolean,
    onCommit: (Boolean) -> Unit
) {
    var showDropDownMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(top = 2.dp)
    ) {
        ConfirmationButton(
            text = "Commit",
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            onClick = { onCommit(false) },
            enabled = canCommit,
            shape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
        )
        Spacer(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp),
        )

        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(MaterialTheme.shapes.small.copy(topStart = CornerSize(0.dp), bottomStart = CornerSize(0.dp)))
                .background(MaterialTheme.colors.primary)
                .handMouseClickable { showDropDownMenu = true }
        ) {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .align(Alignment.Center),
            )
            DropdownMenu(
                onDismissRequest = {
                    showDropDownMenu = false
                },
                content = {
                    DropDownContent(
                        enabled = canAmend,
                        dropDownContentData = DropDownContentData(
                            label = "Amend previous commit",
                            icon = null,
                            onClick = { onCommit(true) }
                        ),
                        onDismiss = { showDropDownMenu = false }
                    )
                },
                expanded = showDropDownMenu,
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
        modifier = Modifier.fillMaxWidth()
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            onClick = onAbort
        )

        ConfirmationButton(
            text = "Merge",
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
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
        modifier = Modifier.fillMaxWidth()
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            onClick = onAbort
        )

        ConfirmationButton(
            text = "Commit",
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            enabled = haveConflictsBeenSolved,
            onClick = onCommit,
        )
    }
}

@Composable
fun RebasingButtons(
    canContinue: Boolean,
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            onClick = onAbort
        )

        if (canContinue) {
            ConfirmationButton(
                text = "Continue",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                enabled = haveConflictsBeenSolved,
                onClick = onContinue,
            )
        } else {
            ConfirmationButton(
                text = "Skip",
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                onClick = onSkip,
            )
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
        modifier = Modifier.fillMaxWidth()
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
                .padding(start = 4.dp),
            enabled = canCommit && haveConflictsBeenSolved,
            onClick = onCommit,
        )
    }
}

@Composable
fun AbortButton(modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.abortButton,
        )
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
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onPrimary),
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
    statusEntries: List<StatusEntry>,
    lazyListState: LazyListState,
    onDiffEntrySelected: (StatusEntry) -> Unit,
    onDiffEntryOptionSelected: (StatusEntry) -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
    onAllAction: () -> Unit,
    allActionTitle: String,
    selectedEntryType: DiffEntryType?,
) {
    Column(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .height(34.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.headerBackground)
        ) {
            Text(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .fillMaxWidth(),
                text = title,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colors.headerText,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            SecondaryButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                text = allActionTitle,
                backgroundButton = actionColor,
                textColor = actionTextColor,
                onClick = onAllAction
            )
        }

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            state = lazyListState,
        ) {
            items(statusEntries) { statusEntry ->
                val isEntrySelected = selectedEntryType != null &&
                        selectedEntryType is DiffEntryType.UncommitedDiff && // Added for smartcast
                        selectedEntryType.statusEntry == statusEntry
                FileEntry(
                    statusEntry = statusEntry,
                    isSelected = isEntrySelected,
                    actionTitle = actionTitle,
                    actionColor = actionColor,
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
private fun FileEntry(
    statusEntry: StatusEntry,
    isSelected: Boolean,
    actionTitle: String,
    actionColor: Color,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .handMouseClickable { onClick() }
            .fillMaxWidth()
            .hoverable(hoverInteraction)
    ) {
        ContextMenu(
            items = {
                onGenerateContextMenu(statusEntry)
            },
        ) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Icon(
                    imageVector = statusEntry.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp),
                    tint = statusEntry.iconColor,
                )

                if (statusEntry.parentDirectoryPath.isNotEmpty()) {
                    Text(
                        text = statusEntry.parentDirectoryPath,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colors.secondaryTextColor,
                    )
                }
                Text(
                    text = statusEntry.fileName,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }
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
}