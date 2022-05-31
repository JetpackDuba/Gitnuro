@file:OptIn(ExperimentalSplitPaneApi::class)

package app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.git.DiffEntryType
import app.theme.borderColor
import app.theme.primaryTextColor
import app.ui.dialogs.NewBranchDialog
import app.ui.dialogs.RebaseInteractive
import app.ui.log.Log
import app.viewmodels.BlameState
import app.viewmodels.TabViewModel
import openRepositoryDialog
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState


@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun RepositoryOpenPage(tabViewModel: TabViewModel) {
    val repositoryState by tabViewModel.repositoryState.collectAsState()
    val diffSelected by tabViewModel.diffSelected.collectAsState()
    val selectedItem by tabViewModel.selectedItem.collectAsState()
    val blameState by tabViewModel.blameState.collectAsState()
    val showHistory by tabViewModel.showHistory.collectAsState()

    var showNewBranchDialog by remember { mutableStateOf(false) }

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
        if (repositoryState == RepositoryState.REBASING_INTERACTIVE) {
            val rebaseInteractiveViewModel = tabViewModel.rebaseInteractiveViewModel

            // TODO Implement continue rebase interactive when gitnuro has been closed
            if (rebaseInteractiveViewModel != null) {
                RebaseInteractive(rebaseInteractiveViewModel)
            } else {
                Text("Rebase started externally", color = MaterialTheme.colors.primaryTextColor)
            }
        } else {
            Menu(
                menuViewModel = tabViewModel.menuViewModel,
                onRepositoryOpen = {
                    openRepositoryDialog(tabViewModel = tabViewModel)
                },
                onCreateBranch = { showNewBranchDialog = true }
            )

            RepoContent(tabViewModel, diffSelected, selectedItem, repositoryState, blameState, showHistory)
        }

    }
}

@Composable
fun RepoContent(
    tabViewModel: TabViewModel,
    diffSelected: DiffEntryType?,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
    showHistory: Boolean,
) {
    if(showHistory) {
        val historyViewModel = tabViewModel.historyViewModel

        if(historyViewModel != null) {
            FileHistory(
                historyViewModel = historyViewModel,
                onClose = {
                    tabViewModel.closeHistory()
                }
            )
        }
    } else {
        MainContentView(
            tabViewModel,
            diffSelected,
            selectedItem,
            repositoryState,
            blameState,
        )
    }


}

@Composable
fun MainContentView(
    tabViewModel: TabViewModel,
    diffSelected: DiffEntryType?,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState
) {
    Row {
        HorizontalSplitPane {
            first(minSize = 350.dp) {
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
                            if (blameState is BlameState.Loaded && !blameState.isMinimized) {
                                Blame(
                                    filePath = blameState.filePath,
                                    blameResult = blameState.blameResult,
                                    onClose = { tabViewModel.resetBlameState() },
                                    onSelectCommit = { tabViewModel.selectCommit(it) }
                                )
                            } else {
                                Column {
                                    Box(modifier = Modifier.weight(1f, true)) {
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

                                    if (blameState is BlameState.Loaded) { // BlameState.isMinimized is true here
                                        MinimizedBlame(
                                            filePath = blameState.filePath,
                                            onExpand = { tabViewModel.expandBlame() },
                                            onClose = { tabViewModel.resetBlameState() }
                                        )
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
                            val safeSelectedItem = selectedItem
                            if (safeSelectedItem == SelectedItem.UncommitedChanges) {
                                UncommitedChanges(
                                    statusViewModel = tabViewModel.statusViewModel,
                                    selectedEntryType = diffSelected,
                                    repositoryState = repositoryState,
                                    onStagedDiffEntrySelected = { diffEntry ->
                                        tabViewModel.minimizeBlame()

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
                                        tabViewModel.minimizeBlame()

                                        if (repositoryState == RepositoryState.SAFE)
                                            tabViewModel.newDiffSelected = DiffEntryType.SafeUnstagedDiff(diffEntry)
                                        else
                                            tabViewModel.newDiffSelected = DiffEntryType.UnsafeUnstagedDiff(diffEntry)
                                    },
                                    onBlameFile = { tabViewModel.blameFile(it) },
                                    onHistoryFile = { tabViewModel.fileHistory(it) }
                                )
                            } else if (safeSelectedItem is SelectedItem.CommitBasedItem) {
                                CommitChanges(
                                    commitChangesViewModel = tabViewModel.commitChangesViewModel,
                                    selectedItem = safeSelectedItem,
                                    diffSelected = diffSelected,
                                    onDiffSelected = { diffEntry ->
                                        tabViewModel.minimizeBlame()
                                        tabViewModel.newDiffSelected = DiffEntryType.CommitDiff(diffEntry)
                                    },
                                    onBlame = { tabViewModel.blameFile(it) },
                                    onHistory = { tabViewModel.fileHistory(it) },
                                )
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