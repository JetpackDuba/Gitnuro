@file:OptIn(ExperimentalSplitPaneApi::class)

package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.git.DiffEntryType
import app.theme.*
import app.ui.dialogs.NewBranchDialog
import app.ui.dialogs.RebaseInteractive
import app.ui.dialogs.StashWithMessageDialog
import app.ui.log.Log
import app.viewmodels.BlameState
import app.viewmodels.TabViewModel
import openRepositoryDialog
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.SplitterScope
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.Cursor


@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun RepositoryOpenPage(tabViewModel: TabViewModel) {
    val repositoryState by tabViewModel.repositoryState.collectAsState()
    val diffSelected by tabViewModel.diffSelected.collectAsState()
    val selectedItem by tabViewModel.selectedItem.collectAsState()
    val blameState by tabViewModel.blameState.collectAsState()
    val showHistory by tabViewModel.showHistory.collectAsState()
    val userInfo by tabViewModel.userInfo.collectAsState()

    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showStashWithMessageDialog by remember { mutableStateOf(false) }

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
    } else if (showStashWithMessageDialog) {
        StashWithMessageDialog(
            onReject = {
                showStashWithMessageDialog = false
            },
            onAccept = { stashMessage ->
                tabViewModel.menuViewModel.stashWithMessage(stashMessage)
                showStashWithMessageDialog = false
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
            Column(modifier = Modifier.weight(1f)) {
                Menu(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 8.dp) // Linear progress bar already take 4 additional dp for top
                        .fillMaxWidth(),
                    menuViewModel = tabViewModel.menuViewModel,
                    onRepositoryOpen = {
                        openRepositoryDialog(tabViewModel = tabViewModel)
                    },
                    onCreateBranch = { showNewBranchDialog = true },
                    onStashWithMessage = { showStashWithMessageDialog = true },
                )

                RepoContent(tabViewModel, diffSelected, selectedItem, repositoryState, blameState, showHistory)
            }
        }

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.2f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${userInfo.name ?: "Name not set"} <${userInfo.email?: "Email not set"}>",
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 12.sp,
            )
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
    if (showHistory) {
        val historyViewModel = tabViewModel.historyViewModel

        if (historyViewModel != null) {
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
            first(minSize = 250.dp) {
                Column(
                    modifier = Modifier
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

            splitter {
                this.repositorySplitter()
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
                                                val diffViewModel = tabViewModel.diffViewModel

                                                if (diffViewModel != null) {
                                                    Diff(
                                                        diffViewModel = diffViewModel,
                                                        onCloseDiffView = {
                                                            tabViewModel.newDiffSelected = null
                                                        }
                                                    )
                                                }
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

                    splitter {
                        this.repositorySplitter()
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

fun SplitterScope.repositorySplitter() {
    visiblePart {
        Box(
            Modifier
                .width(8.dp)
                .fillMaxHeight()
                .background(Color.Transparent)
        )
    }
    handle {
        Box(
            Modifier
                .markAsHandle()
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .background(Color.Transparent)
                .width(8.dp)
                .fillMaxHeight()
        )
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