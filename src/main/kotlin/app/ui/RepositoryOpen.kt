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
import app.DialogManager
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
fun RepositoryOpenPage(gitManager: GitManager, dialogManager: DialogManager) {
    var selectedRevCommit by remember {
        mutableStateOf<RevCommit?>(null)
    }

    var diffSelected by remember {
        mutableStateOf<DiffEntryType?>(null)
    }
    var uncommitedChangesSelected by remember {
        mutableStateOf(false)
    }

    val selectedIndexCommitLog = remember { mutableStateOf(-1) }

    Column {
        GMenu(
            onRepositoryOpen = {
                openRepositoryDialog(gitManager = gitManager)
            },
            onPull = { gitManager.pull() },
            onPush = { gitManager.push() },
            onStash = { gitManager.stash() },
            onPopStash = { gitManager.popStash() },
            onCreateBranch = {
                dialogManager.show {
                    NewBranchDialog(
                        onReject = {
                            dialogManager.dismiss()
                        },
                        onAccept = { branchName ->
                            gitManager.createBranch(branchName)
                            dialogManager.dismiss()
                        }
                    )
                }
            }
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
                        Branches(gitManager = gitManager)
                        Tags(gitManager = gitManager)
                        Stashes(gitManager = gitManager)
                    }
                }

                splitter {
                    visiblePart {
                        Box(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colors.background)
                        )
                    }
                    handle {
                        Box(
                            Modifier
                                .markAsHandle()
                                .background(SolidColor(Color.Gray), alpha = 0.50f)
                                .width(2.dp)
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                                .fillMaxHeight()
                        )
                    }
                }
                second {
                    HorizontalSplitPane(
                        splitPaneState = rememberSplitPaneState(0.9f)
                    ) {
                        first() {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Crossfade(targetState = diffSelected) { diffEntry ->
                                    when (diffEntry) {
                                        null -> {
                                            Log(
                                                gitManager = gitManager,
                                                dialogManager = dialogManager,
                                                selectedIndex = selectedIndexCommitLog,
                                                onRevCommitSelected = { commit ->
                                                    selectedRevCommit = commit
                                                    uncommitedChangesSelected = false
                                                },
                                                onUncommitedChangesSelected = {
                                                    gitManager.statusShouldBeUpdated()
                                                    uncommitedChangesSelected = true
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
                        splitter {
                            visiblePart {
                                Box(
                                    Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colors.background)
                                )
                            }
                            handle {
                                Box(
                                    Modifier
                                        .markAsHandle()
                                        .background(SolidColor(Color.Gray), alpha = 0.50f)
                                        .width(2.dp)
                                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                                        .fillMaxHeight()
                                )
                            }
                        }
                        second(minSize = 300.dp) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                            ) {
                                if (uncommitedChangesSelected) {
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
                                } else {
                                    selectedRevCommit?.let {
                                        CommitChanges(
                                            gitManager = gitManager,
                                            commit = it,
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
}