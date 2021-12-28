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
import app.extensions.icon
import app.extensions.iconColor
import app.git.DiffEntryType
import app.git.GitManager
import app.git.StageStatus
import app.theme.headerBackground
import app.theme.headerText
import app.theme.primaryTextColor
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SecondaryButton
import org.eclipse.jgit.diff.DiffEntry

@OptIn(ExperimentalAnimationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun UncommitedChanges(
    gitManager: GitManager,
    selectedEntryType: DiffEntryType?,
    onStagedDiffEntrySelected: (DiffEntry?) -> Unit,
    onUnstagedDiffEntrySelected: (DiffEntry) -> Unit,
) {
    val stageStatusState = gitManager.stageStatus.collectAsState()
    val stageStatus = stageStatusState.value
    val lastCheck by gitManager.lastTimeChecked.collectAsState()
    val repositoryState by gitManager.repositoryState.collectAsState()

    LaunchedEffect(lastCheck) {
        gitManager.loadStatus()
    }

    val staged: List<DiffEntry>
    val unstaged: List<DiffEntry>
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
        staged = listOf<DiffEntry>()
        unstaged = listOf<DiffEntry>() // return empty lists if still loading
    }


    var commitMessage by remember { mutableStateOf("") }
    val doCommit = {
        gitManager.commit(commitMessage)
        onStagedDiffEntrySelected(null)
        commitMessage = ""
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
                gitManager.unstage(it)
            },
            onReset = { diffEntry ->
                gitManager.resetStaged(diffEntry)
            },
            onAllAction = {
                gitManager.unstageAll()
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
                gitManager.stage(it)
            },
            onReset = { diffEntry ->
                gitManager.resetUnstaged(diffEntry)
            },
            {
                gitManager.stageAll()
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
                    onValueChange = { commitMessage = it },
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
                    Text(
                        text = "Commit",
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

fun checkIfSelectedEntryShouldBeUpdated(
    selectedEntryType: DiffEntryType,
    staged: List<DiffEntry>,
    unstaged: List<DiffEntry>,
    onStagedDiffEntrySelected: (DiffEntry?) -> Unit,
    onUnstagedDiffEntrySelected: (DiffEntry) -> Unit,
) {
    val selectedDiffEntry = selectedEntryType.diffEntry
    val selectedEntryTypeNewId = selectedDiffEntry.newId.name()

    if (selectedEntryType is DiffEntryType.StagedDiff) {
        val entryType = staged.firstOrNull { it.newPath == selectedDiffEntry.newPath }

        if(
            entryType != null &&
            selectedEntryTypeNewId != entryType.newId.name()
        ) {
            onStagedDiffEntrySelected(entryType)
        } else if (entryType == null)
            onStagedDiffEntrySelected(null)
    } else if(selectedEntryType is DiffEntryType.UnstagedDiff) {
        val entryType = unstaged.firstOrNull {
            if(selectedDiffEntry.changeType == DiffEntry.ChangeType.DELETE)
                it.oldPath == selectedDiffEntry.oldPath
            else
                it.newPath == selectedDiffEntry.newPath
        }

        if(entryType != null) {
            onUnstagedDiffEntrySelected(entryType)
        } else onStagedDiffEntrySelected(null)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EntriesList(
    modifier: Modifier,
    title: String,
    actionTitle: String,
    actionColor: Color,
    diffEntries: List<DiffEntry>,
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
            itemsIndexed(diffEntries) { index, diffEntry ->
                FileEntry(
                    diffEntry = diffEntry,
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
    diffEntry: DiffEntry,
    actionTitle: String,
    actionColor: Color,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    onReset: () -> Unit,
) {
    var active by remember { mutableStateOf(false) }

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
                    imageVector = diffEntry.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp),
                    tint = diffEntry.iconColor,
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