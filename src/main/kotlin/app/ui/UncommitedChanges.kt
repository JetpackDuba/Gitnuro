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
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.ContextMenuItem
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ui.components.ScrollableLazyColumn
import app.extensions.filePath
import app.extensions.icon
import app.extensions.iconColor
import app.git.GitManager
import app.git.StageStatus
import org.eclipse.jgit.diff.DiffEntry
import app.theme.headerBackground

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UncommitedChanges(
    gitManager: GitManager,
    onStagedDiffEntrySelected: (DiffEntry) -> Unit,
    onUnstagedDiffEntrySelected: (DiffEntry) -> Unit,
) {
    val stageStatusState = gitManager.stageStatus.collectAsState()
    val stageStatus = stageStatusState.value
    val lastCheck by gitManager.lastTimeChecked.collectAsState()

    LaunchedEffect(lastCheck) {
        gitManager.loadStatus()
    }

    val (staged, unstaged) = if (stageStatus is StageStatus.Loaded) {
        stageStatus.staged to stageStatus.unstaged
    } else {
        listOf<DiffEntry>() to listOf<DiffEntry>() // return 2 empty lists if still loading
    }

    var commitMessage by remember { mutableStateOf("") }

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
            actionTitle = "Unstage",
            actionColor = MaterialTheme.colors.error,
            diffEntries = staged,
            onDiffEntrySelected = onStagedDiffEntrySelected,
            onDiffEntryOptionSelected = {
                gitManager.unstage(it)
            },
            onReset = { diffEntry ->
                gitManager.resetStaged(diffEntry)
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
            }
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
                        .weight(weight = 1f, fill = true),
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text("Write your commit message here", fontSize = 14.sp) },
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.surface),
                    textStyle = TextStyle.Default.copy(fontSize = 14.sp)
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        gitManager.commit(commitMessage)
                        commitMessage = ""
                    },
                    enabled = commitMessage.isNotEmpty() && staged.isNotEmpty(),
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
) {

    Card(
        modifier = modifier
    ) {
        Column {
            Text(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.headerBackground)
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 14.sp,
                maxLines = 1,
            )

            ScrollableLazyColumn(
                modifier = Modifier.fillMaxSize(),
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
}

@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalDesktopApi::class, androidx.compose.animation.ExperimentalAnimationApi::class
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