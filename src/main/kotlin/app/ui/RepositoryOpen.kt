package app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import app.git.DiffEntryType
import app.git.GitManager
import app.ui.dialogs.NewBranchDialog
import app.ui.log.Log
import openRepositoryDialog
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.Cursor


@OptIn(ExperimentalSplitPaneApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun RepositoryOpenPage(gitManager: GitManager) {
    var diffSelected by remember {
        mutableStateOf<DiffEntryType?>(null)
    }

    var showNewBranchDialog by remember { mutableStateOf(false) }

    val (selectedItem, setSelectedItem) = remember { mutableStateOf<SelectedItem>(SelectedItem.None) }

    LaunchedEffect(selectedItem) {
        diffSelected = null
    }

    if(showNewBranchDialog) {
        NewBranchDialog(
            onReject = {
                showNewBranchDialog = false
            },
            onAccept = { branchName ->
                gitManager.createBranch(branchName)
                showNewBranchDialog = false
            }
        )
    }

    Column {
        GMenu(
            onRepositoryOpen = {
                openRepositoryDialog(gitManager = gitManager)
            },
            onPull = { gitManager.pull() },
            onPush = { gitManager.push() },
            onStash = { gitManager.stash() },
            onPopStash = { gitManager.popStash() },
            onCreateBranch = { showNewBranchDialog = true }
        )

        Row {
            HorizontalSplitPane() {
                first(minSize = 200.dp) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 300.dp)
                            .weight(0.15f)
                            .fillMaxHeight()
                    ) {
                        Branches(
                            gitManager = gitManager,
                            onBranchClicked = {
                                val commit = gitManager.findCommit(it.objectId)
                                setSelectedItem(SelectedItem.Ref(commit))
                            }
                        )
                        Remotes(gitManager = gitManager)
                        Tags(
                            gitManager = gitManager,
                            onTagClicked = {
                                val commit = gitManager.findCommit(it.objectId)
                                setSelectedItem(SelectedItem.Ref(commit))
                            }
                        )
                        Stashes(
                            gitManager = gitManager,
                            onStashSelected = { stash ->
                                setSelectedItem(SelectedItem.Stash(stash))
                            }
                        )
                    }
                }
                
                second {
                    HorizontalSplitPane(
                        splitPaneState = rememberSplitPaneState(0.9f)
                    ) {
                        first {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Crossfade(targetState = diffSelected) { diffEntry ->
                                    when (diffEntry) {
                                        null -> {
                                            Log(
                                                gitManager = gitManager,
                                                selectedItem = selectedItem,
                                                onItemSelected = {
                                                    setSelectedItem(it)
                                                },
                                            )
                                        }
                                        else -> {
                                            Diff(
                                                gitManager = gitManager,
                                                diffEntryType = diffEntry,
                                                onCloseDiffView = { diffSelected = null })
                                        }
                                    }
                                }
                            }
                        }

                        second(minSize = 300.dp) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                            ) {
                                if (selectedItem == SelectedItem.UncommitedChanges) {
                                    UncommitedChanges(
                                        gitManager = gitManager,
                                        onStagedDiffEntrySelected = { diffEntry ->
                                            diffSelected = if (diffEntry != null)
                                                DiffEntryType.StagedDiff(diffEntry)
                                            else
                                                null
                                        },
                                        onUnstagedDiffEntrySelected = { diffEntry ->
                                            diffSelected = DiffEntryType.UnstagedDiff(diffEntry)
                                        }
                                    )
                                } else if(selectedItem is SelectedItem.CommitBasedItem) {
                                    CommitChanges(
                                        gitManager = gitManager,
                                        commit = selectedItem.revCommit,
                                        onDiffSelected = { diffEntry ->
                                            diffSelected = DiffEntryType.CommitDiff(diffEntry)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class SelectedItem {
    object None : SelectedItem()
    object UncommitedChanges : SelectedItem()
    sealed class CommitBasedItem(val revCommit: RevCommit) : SelectedItem()
    class Ref(revCommit: RevCommit) : CommitBasedItem(revCommit)
    class Commit(revCommit: RevCommit) : CommitBasedItem(revCommit)
    class Stash(revCommit: RevCommit) : CommitBasedItem(revCommit)
}