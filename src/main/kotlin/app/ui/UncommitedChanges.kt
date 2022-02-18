@file:OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.filePath
import app.extensions.isMerging
import app.git.DiffEntryType
import app.git.StatusEntry
import app.theme.*
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SecondaryButton
import app.ui.context_menu.DropDownContent
import app.ui.context_menu.DropDownContentData
import app.ui.context_menu.stagedEntriesContextMenuItems
import app.ui.context_menu.unstagedEntriesContextMenuItems
import app.viewmodels.StageStatus
import app.viewmodels.StatusViewModel
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.RepositoryState

@Composable
fun UncommitedChanges(
    statusViewModel: StatusViewModel,
    selectedEntryType: DiffEntryType?,
    repositoryState: RepositoryState,
    onStagedDiffEntrySelected: (DiffEntry?) -> Unit,
    onUnstagedDiffEntrySelected: (DiffEntry) -> Unit,
) {
    val stageStatusState = statusViewModel.stageStatus.collectAsState()
    val commitMessage by statusViewModel.commitMessage.collectAsState()

    val stageStatus = stageStatusState.value
    val staged: List<StatusEntry>
    val unstaged: List<StatusEntry>

    if (stageStatus is StageStatus.Loaded) {
        staged = stageStatus.staged
        unstaged = stageStatus.unstaged

        LaunchedEffect(staged) {
            if (selectedEntryType != null) {
                checkIfSelectedEntryShouldBeUpdated(
                    selectedEntryType = selectedEntryType,
                    staged = staged,
                    unstaged = unstaged,
                    onStagedDiffEntrySelected = onStagedDiffEntrySelected,
                    onUnstagedDiffEntrySelected = onUnstagedDiffEntrySelected,
                )
            }
        }
    } else {
        staged = listOf()
        unstaged = listOf() // return empty lists if still loading
    }

    val doCommit = { amend: Boolean ->
        statusViewModel.commit(commitMessage, amend)
        onStagedDiffEntrySelected(null)
        statusViewModel.newCommitMessage = ""
    }

    val canCommit = commitMessage.isNotEmpty() && staged.isNotEmpty()
    val canAmend = (commitMessage.isNotEmpty() || staged.isNotEmpty()) && statusViewModel.hasPreviousCommits

    Column {
        AnimatedVisibility(
            visible = stageStatus is StageStatus.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        EntriesList(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                .weight(5f)
                .fillMaxWidth(),
            title = "Staged",
            allActionTitle = "Unstage all",
            actionTitle = "Unstage",
            actionColor = MaterialTheme.colors.unstageButton,
            diffEntries = staged,
            onDiffEntrySelected = onStagedDiffEntrySelected,
            onDiffEntryOptionSelected = {
                statusViewModel.unstage(it)
            },
            onGenerateContextMenu = { diffEntry ->
                stagedEntriesContextMenuItems(
                    diffEntry = diffEntry,
                    onReset = {
                        statusViewModel.resetStaged(diffEntry)
                    },
                )
            },
            onAllAction = {
                statusViewModel.unstageAll()
            }
        )

        EntriesList(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 4.dp)
                .weight(5f)
                .fillMaxWidth(),
            title = "Unstaged",
            actionTitle = "Stage",
            actionColor = MaterialTheme.colors.stageButton,
            diffEntries = unstaged,
            onDiffEntrySelected = onUnstagedDiffEntrySelected,
            onDiffEntryOptionSelected = {
                statusViewModel.stage(it)
            },
            onGenerateContextMenu = { diffEntry ->
                unstagedEntriesContextMenuItems(
                    diffEntry = diffEntry,
                    onReset = {
                        statusViewModel.resetUnstaged(diffEntry)
                    },
                    onDelete = {
                        statusViewModel.deleteFile(diffEntry)
                    }
                )
            },
            allActionTitle = "Stage all",
            onAllAction = {
                statusViewModel.stageAll()
            }
        )

        Column(
            modifier = Modifier
                .padding(8.dp)
                .run {
                    // When rebasing, we don't need a fixed size as we don't show the message TextField
                    if (!repositoryState.isRebasing) {
                        height(192.dp)
                    } else
                        this
                }
                .fillMaxWidth()
        ) {
            // Don't show the message TextField when rebasing as it can't be edited
            if (!repositoryState.isRebasing)
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = true)
                        .onPreviewKeyEvent {
                            if (it.isCtrlPressed && it.key == Key.Enter && canCommit) {
                                doCommit(false)
                                true
                            } else
                                false
                        },
                    value = commitMessage,
                    onValueChange = { statusViewModel.newCommitMessage = it },
                    label = { Text("Write your commit message here", fontSize = 14.sp) },
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.background),
                    textStyle = TextStyle.Default.copy(fontSize = 14.sp, color = MaterialTheme.colors.primaryTextColor),
                )

            when {
                repositoryState.isMerging -> MergeButtons(
                    haveConflictsBeenSolved = unstaged.isEmpty(),
                    onAbort = { statusViewModel.abortMerge() },
                    onMerge = { doCommit(false) }
                )
                repositoryState.isRebasing -> RebasingButtons(
                    canContinue = staged.isNotEmpty() || unstaged.isNotEmpty(),
                    haveConflictsBeenSolved = unstaged.isEmpty(),
                    onAbort = { statusViewModel.abortRebase() },
                    onContinue = { statusViewModel.continueRebase() },
                    onSkip = { statusViewModel.skipRebase() },
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
        Button(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            onClick = { onCommit(false) },
            enabled = canCommit,
            shape = RectangleShape,
        ) {
            Text(
                text = "Commit",
                fontSize = 14.sp,
            )
        }
        Spacer(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp),
        )

        Box(
            modifier = Modifier
                .height(40.dp)
                .background(MaterialTheme.colors.primary)
                .clickable { showDropDownMenu = true },
        ) {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colors.inversePrimaryTextColor,
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
//        }
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
                .padding(start = 8.dp, end = 4.dp),
            onClick = onAbort
        )

        Button(
            onClick = onMerge,
            enabled = haveConflictsBeenSolved,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 4.dp),
        ) {
            Text(
                text = "Merge",
                fontSize = 14.sp,
            )
        }

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
                .padding(start = 8.dp, end = 4.dp),
            onClick = onAbort
        )

        if (canContinue) {
            Button(
                onClick = onContinue,
                enabled = haveConflictsBeenSolved,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 4.dp),
            ) {
                Text(
                    text = "Continue",
                    fontSize = 14.sp,
                )
            }
        } else {
            Button(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 4.dp),
            ) {
                Text(
                    text = "Skip",
                    fontSize = 14.sp,
                )
            }
        }

    }
}

@Composable
fun AbortButton(modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.unstageButton,
            contentColor = Color.White
        )
    ) {
        Text(
            text = "Abort",
            fontSize = 14.sp,
        )
    }
}

// TODO: This logic should be part of the diffViewModel where it gets the latest version of the diffEntry
fun checkIfSelectedEntryShouldBeUpdated(
    selectedEntryType: DiffEntryType,
    staged: List<StatusEntry>,
    unstaged: List<StatusEntry>,
    onStagedDiffEntrySelected: (DiffEntry?) -> Unit,
    onUnstagedDiffEntrySelected: (DiffEntry) -> Unit,
) {
    val selectedDiffEntry = selectedEntryType.diffEntry
    val selectedEntryTypeNewId = selectedDiffEntry.newId.name()

    if (selectedEntryType is DiffEntryType.StagedDiff) {
        val entryType =
            staged.firstOrNull { stagedEntry -> stagedEntry.diffEntry.newPath == selectedDiffEntry.newPath }?.diffEntry

        if (
            entryType != null &&
            selectedEntryTypeNewId != entryType.newId.name()
        ) {
            onStagedDiffEntrySelected(entryType)

        } else if (entryType == null) {
            onStagedDiffEntrySelected(null)
        }
    } else if (selectedEntryType is DiffEntryType.UnstagedDiff) {
        val entryType = unstaged.firstOrNull { unstagedEntry ->
            if (selectedDiffEntry.changeType == DiffEntry.ChangeType.DELETE)
                unstagedEntry.diffEntry.oldPath == selectedDiffEntry.oldPath
            else
                unstagedEntry.diffEntry.newPath == selectedDiffEntry.newPath
        }

        if (entryType != null) {
            onUnstagedDiffEntrySelected(entryType.diffEntry)
        } else
            onStagedDiffEntrySelected(null)
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun EntriesList(
    modifier: Modifier,
    title: String,
    actionTitle: String,
    actionColor: Color,
    diffEntries: List<StatusEntry>,
    onDiffEntrySelected: (DiffEntry) -> Unit,
    onDiffEntryOptionSelected: (DiffEntry) -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuItem>,
    onAllAction: () -> Unit,
    allActionTitle: String,
) {
    Column(
        modifier = modifier
    ) {
        Box {
            Text(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.headerBackground)
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                text = title,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.headerText,
                fontSize = 13.sp,
                maxLines = 1,
            )

            SecondaryButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                text = allActionTitle,
                backgroundButton = actionColor,
                onClick = onAllAction
            )
        }

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
        ) {
            itemsIndexed(diffEntries) { index, statusEntry ->
                val diffEntry = statusEntry.diffEntry
                FileEntry(
                    statusEntry = statusEntry,
                    actionTitle = actionTitle,
                    actionColor = actionColor,
                    onClick = {
                        onDiffEntrySelected(diffEntry)
                    },
                    onButtonClick = {
                        onDiffEntryOptionSelected(diffEntry)
                    },
                    onGenerateContextMenu = onGenerateContextMenu,
                )

                if (index < diffEntries.size - 1) {
                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
private fun FileEntry(
    statusEntry: StatusEntry,
    actionTitle: String,
    actionColor: Color,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuItem>,
) {
    var active by remember { mutableStateOf(false) }
    val diffEntry = statusEntry.diffEntry

    Box(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .pointerMoveFilter(
                onEnter = {
                    active = true
                    false
                },
                onExit = {
                    active = false
                    false
                }
            )
    ) {
        ContextMenuArea(
            items = {
                onGenerateContextMenu(diffEntry)
            },
        ) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth(),
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

                Text(
                    text = diffEntry.filePath,
                    modifier = Modifier.weight(1f, fill = true),
                    maxLines = 1,
                    softWrap = false,
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.primaryTextColor,
                )
            }
        }
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            visible = active,
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