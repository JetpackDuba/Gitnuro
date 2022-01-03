package app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.git.DiffEntryType
import app.viewmodels.TabViewModel
import app.ui.dialogs.NewBranchDialog
import app.ui.log.Log
import openRepositoryDialog
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState


@OptIn(ExperimentalSplitPaneApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun RepositoryOpenPage(tabViewModel: TabViewModel) {
    val repositoryState by tabViewModel.repositoryState.collectAsState()
    val diffSelected by tabViewModel.diffSelected.collectAsState()

    var showNewBranchDialog by remember { mutableStateOf(false) }

    val (selectedItem, setSelectedItem) = remember { mutableStateOf<SelectedItem>(SelectedItem.None) }
    LaunchedEffect(selectedItem) {
        tabViewModel.newDiffSelected = null
    }

    if (showNewBranchDialog) {
        NewBranchDialog(
            onReject = {
                showNewBranchDialog = false
            },
            onAccept = { branchName ->
                tabViewModel.branchesViewModel.createBranch(branchName)
                showNewBranchDialog = false
            }
        )
    }

    Column {
        GMenu(
            onRepositoryOpen = {
                openRepositoryDialog(gitManager = tabViewModel)
            },
            onPull = { tabViewModel.pull() },
            onPush = { tabViewModel.push() },
            onStash = { tabViewModel.stash() },
            onPopStash = { tabViewModel.popStash() },
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
                            branchesViewModel = tabViewModel.branchesViewModel,
                            onBranchClicked = {
                                val commit = tabViewModel.findCommit(it.objectId)
                                setSelectedItem(SelectedItem.Ref(commit))
                            }
                        )
                        Remotes(remotesViewModel = tabViewModel.remotesViewModel)
                        Tags(
                            tagsViewModel = tabViewModel.tagsViewModel,
                            onTagClicked = {
                                val commit = tabViewModel.findCommit(it.objectId)
                                setSelectedItem(SelectedItem.Ref(commit))
                            }
                        )
                        Stashes(
                            gitManager = tabViewModel,
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
                                when (diffSelected) {
                                    null -> {
                                        Log(
                                            tabViewModel = tabViewModel,
                                            repositoryState = repositoryState,
                                            logViewModel = tabViewModel.logViewModel,
                                            selectedItem = selectedItem,
                                            onItemSelected = {
                                                setSelectedItem(it)
                                            },
                                        )
                                    }
                                    else -> {
                                        Diff(
                                            diffViewModel = tabViewModel.diffViewModel,
                                            onCloseDiffView = { tabViewModel.newDiffSelected = null })
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
                                        statusViewModel = tabViewModel.statusViewModel,
                                        selectedEntryType = diffSelected,
                                        repositoryState = repositoryState,
                                        onStagedDiffEntrySelected = { diffEntry ->
                                            tabViewModel.newDiffSelected = if (diffEntry != null)
                                                DiffEntryType.StagedDiff(diffEntry)
                                            else
                                                null
                                        },
                                        onUnstagedDiffEntrySelected = { diffEntry ->
                                            tabViewModel.newDiffSelected = DiffEntryType.UnstagedDiff(diffEntry)
                                        }
                                    )
                                } else if (selectedItem is SelectedItem.CommitBasedItem) {
                                    CommitChanges(
                                        gitManager = tabViewModel,
                                        commit = selectedItem.revCommit,
                                        onDiffSelected = { diffEntry ->
                                            tabViewModel.newDiffSelected = DiffEntryType.CommitDiff(diffEntry)
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