@file:Suppress("UNUSED_PARAMETER")

package app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.filePath
import app.extensions.isMerging
import app.git.DiffEntryType
import app.git.StatusEntry
import app.theme.headerBackground
import app.theme.headerText
import app.theme.primaryTextColor
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SecondaryButton
import app.viewmodels.StageStatus
import app.viewmodels.StatusViewModel
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.RepositoryState

@OptIn(ExperimentalAnimationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
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
            if(selectedEntryType != null) {
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
        staged = listOf<StatusEntry>()
        unstaged = listOf<StatusEntry>() // return empty lists if still loading
    }

    val doCommit = {
        statusViewModel.commit(commitMessage)
        onStagedDiffEntrySelected(null)
        statusViewModel.newCommitMessage = ""
    }
    val canCommit = commitMessage.isNotEmpty() && staged.isNotEmpty()

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
                .padding(8.dp)
                .weight(5f)
                .fillMaxWidth(),
            title = "Staged",
            allActionTitle = "Unstage all",
            actionTitle = "Unstage",
            actionColor = MaterialTheme.colors.error,
            diffEntries = staged,
            onDiffEntrySelected = onStagedDiffEntrySelected,
            onDiffEntryOptionSelected = {
                statusViewModel.unstage(it)
            },
            onReset = { diffEntry ->
                statusViewModel.resetStaged(diffEntry)
            },
            onAllAction = {
                statusViewModel.unstageAll()
            }
        )

        EntriesList(
            modifier = Modifier
                .padding(8.dp)
                .weight(5f)
                .fillMaxWidth(),
            title = "Unstaged",
            actionTitle = "Stage",
            actionColor = MaterialTheme.colors.primary,
            diffEntries = unstaged,
            onDiffEntrySelected = onUnstagedDiffEntrySelected,
            onDiffEntryOptionSelected = {
                statusViewModel.stage(it)
            },
            onReset = { diffEntry ->
                statusViewModel.resetUnstaged(diffEntry)
            },
            {
                statusViewModel.stageAll()
            },
            allActionTitle = "Stage all"
        )

        Card(
            modifier = Modifier
                .padding(8.dp)
                .height(192.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = true)
                        .onPreviewKeyEvent {
                            if (it.isCtrlPressed && it.key == Key.Enter && canCommit) {
                                doCommit()
                                true
                            } else
                                false
                        },
                    value = commitMessage,
                    onValueChange = { statusViewModel.newCommitMessage = it },
                    label = { Text("Write your commit message here", fontSize = 14.sp) },
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.background),
                    textStyle = TextStyle.Default.copy(fontSize = 14.sp),
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        doCommit()
                    },
                    enabled = canCommit,
                    shape = RectangleShape,
                ) {
                    val buttonText = if(repositoryState.isMerging)
                        "Merge"
                    else if (repositoryState.isRebasing)
                        "Continue rebasing"
                    else
                        "Commit"
                    Text(
                        text = buttonText,
                        fontSize = 14.sp,
                    )
                }
            }
        }
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
        val entryType = staged.firstOrNull { stagedEntry -> stagedEntry.diffEntry.newPath == selectedDiffEntry.newPath }?.diffEntry

        if(
            entryType != null &&
            selectedEntryTypeNewId != entryType.newId.name()
        ) {
            onStagedDiffEntrySelected(entryType)

        } else if (entryType == null) {
            onStagedDiffEntrySelected(null)
        }
    } else if(selectedEntryType is DiffEntryType.UnstagedDiff) {
        val entryType = unstaged.firstOrNull { unstagedEntry ->
            if(selectedDiffEntry.changeType == DiffEntry.ChangeType.DELETE)
                unstagedEntry.diffEntry.oldPath == selectedDiffEntry.oldPath
            else
                unstagedEntry.diffEntry.newPath == selectedDiffEntry.newPath
        }

        if(entryType != null) {
            onUnstagedDiffEntrySelected(entryType.diffEntry)
        } else
            onStagedDiffEntrySelected(null)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EntriesList(
    modifier: Modifier,
    title: String,
    actionTitle: String,
    actionColor: Color,
    diffEntries: List<StatusEntry>,
    onDiffEntrySelected: (DiffEntry) -> Unit,
    onDiffEntryOptionSelected: (DiffEntry) -> Unit,
    onReset: (DiffEntry) -> Unit,
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
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.headerText,
                fontSize = 14.sp,
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
                    onReset = {
                        onReset(diffEntry)
                    }
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
    onReset: () -> Unit,
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
                listOf(
                    ContextMenuItem(
                        label = "Reset",
                        onClick = onReset
                    )
                )
            },
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
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
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
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
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = actionColor)
            ) {
                Text(actionTitle, fontSize = 12.sp)
            }
        }
    }
}