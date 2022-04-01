package app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.git.DiffEntryType
import app.theme.borderColor
import app.ui.dialogs.NewBranchDialog
import app.ui.log.Log
import app.viewmodels.TabViewModel
import openRepositoryDialog
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState


@OptIn(ExperimentalSplitPaneApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun RepositoryOpenPage(tabViewModel: TabViewModel) {
    val repositoryState by tabViewModel.repositoryState.collectAsState()
    val diffSelected by tabViewModel.diffSelected.collectAsState()
    val selectedItem by tabViewModel.selectedItem.collectAsState()

    var showNewBranchDialog by remember { mutableStateOf(false) }
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
        Menu(
            menuViewModel = tabViewModel.menuViewModel,
            onRepositoryOpen = {
                openRepositoryDialog(tabViewModel = tabViewModel)
            },
            onCreateBranch = { showNewBranchDialog = true }
        )

        Row {
            HorizontalSplitPane {
                first(minSize = 200.dp) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 300.dp)
                            .weight(0.15f)
                            .fillMaxHeight()
                    ) {
                        Branches(
                            branchesViewModel = tabViewModel.branchesViewModel,
                        )
                        Remotes(
                            remotesViewModel = tabViewModel.remotesViewModel,
                        )
                        Tags(
                            tagsViewModel = tabViewModel.tagsViewModel,
                        )
                        Stashes(
                            stashesViewModel = tabViewModel.stashesViewModel,
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
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colors.borderColor,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            ) {
                                when (diffSelected) {
                                    null -> {
                                        Log(
                                            logViewModel = tabViewModel.logViewModel,
                                            selectedItem = selectedItem,
                                            repositoryState = repositoryState,
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
                                val safeSelectedItem = selectedItem
                                if (safeSelectedItem == SelectedItem.UncommitedChanges) {
                                    UncommitedChanges(
                                        statusViewModel = tabViewModel.statusViewModel,
                                        selectedEntryType = diffSelected,
                                        repositoryState = repositoryState,
                                        onStagedDiffEntrySelected = { diffEntry ->
                                            tabViewModel.newDiffSelected = if (diffEntry != null) {
                                                if (repositoryState == RepositoryState.SAFE)
                                                    DiffEntryType.SafeStagedDiff(diffEntry)
                                                else
                                                    DiffEntryType.UnsafeStagedDiff(diffEntry)
                                            } else {
                                                null
                                            }
                                        },
                                        onUnstagedDiffEntrySelected = { diffEntry ->
                                            if (repositoryState == RepositoryState.SAFE)
                                                tabViewModel.newDiffSelected = DiffEntryType.SafeUnstagedDiff(diffEntry)
                                            else
                                                tabViewModel.newDiffSelected = DiffEntryType.UnsafeUnstagedDiff(diffEntry)
                                        }
                                    )
                                } else if (safeSelectedItem is SelectedItem.CommitBasedItem) {
                                    CommitChanges(
                                        commitChangesViewModel = tabViewModel.commitChangesViewModel,
                                        selectedItem = safeSelectedItem,
                                        diffSelected = diffSelected,
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