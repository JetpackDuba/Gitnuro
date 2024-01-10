@file:OptIn(ExperimentalSplitPaneApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppConstants
import com.jetpackduba.gitnuro.LocalTabScope
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.ui.components.SecondaryButton
import com.jetpackduba.gitnuro.ui.components.TripleVerticalSplitPanel
import com.jetpackduba.gitnuro.ui.components.gitnuroDynamicViewModel
import com.jetpackduba.gitnuro.ui.dialogs.*
import com.jetpackduba.gitnuro.ui.diff.Diff
import com.jetpackduba.gitnuro.ui.log.Log
import com.jetpackduba.gitnuro.viewmodels.BlameState
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi

@Composable
fun RepositoryOpenPage(
    tabViewModel: TabViewModel,
    onShowSettingsDialog: () -> Unit,
    onShowCloneDialog: () -> Unit,
) {
    val repositoryState by tabViewModel.repositoryState.collectAsState()
    val diffSelected by tabViewModel.diffSelected.collectAsState()
    val selectedItem by tabViewModel.selectedItem.collectAsState()
    val blameState by tabViewModel.blameState.collectAsState()
    val showHistory by tabViewModel.showHistory.collectAsState()
    val showAuthorInfo by tabViewModel.showAuthorInfo.collectAsState()

    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showStashWithMessageDialog by remember { mutableStateOf(false) }
    var showQuickActionsDialog by remember { mutableStateOf(false) }
    var showSignOffDialog by remember { mutableStateOf(false) }

    if (showNewBranchDialog) {
        NewBranchDialog(
            onClose = {
                showNewBranchDialog = false
            },
            onAccept = { branchName ->
                tabViewModel.createBranch(branchName)
                showNewBranchDialog = false
            }
        )
    } else if (showStashWithMessageDialog) {
        StashWithMessageDialog(
            onClose = {
                showStashWithMessageDialog = false
            },
            onAccept = { stashMessage ->
                tabViewModel.stashWithMessage(stashMessage)
                showStashWithMessageDialog = false
            }
        )
    } else if (showAuthorInfo) {
        val authorViewModel = tabViewModel.authorViewModel
        if (authorViewModel != null) {
            AuthorDialog(
                authorViewModel = authorViewModel,
                onClose = {
                    tabViewModel.closeAuthorInfoDialog()
                }
            )
        }
    } else if (showQuickActionsDialog) {
        QuickActionsDialog(
            onClose = { showQuickActionsDialog = false },
            onAction = {
                showQuickActionsDialog = false
                when (it) {
                    QuickActionType.OPEN_DIR_IN_FILE_MANAGER -> tabViewModel.openFolderInFileExplorer()
                    QuickActionType.CLONE -> onShowCloneDialog()
                    QuickActionType.REFRESH -> tabViewModel.refreshAll()
                    QuickActionType.SIGN_OFF -> showSignOffDialog = true
                }
            },
        )
    } else if (showSignOffDialog) {
        SignOffDialog(
            viewModel = gitnuroDynamicViewModel(),
            onClose = { showSignOffDialog = false },
        )
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedItem) {
        focusRequester.requestFocus()
    }
    Column {
        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.matchesBinding(KeybindingOption.REFRESH)) {
                            tabViewModel.refreshAll()
                            true
                        } else {
                            false
                        }
                    }
            ) {
                val currentTabInformation = LocalTabScope.current
                Column(modifier = Modifier.weight(1f)) {
                    Menu(
                        modifier = Modifier
                            .padding(
                                vertical = 4.dp
                            )
                            .fillMaxWidth(),
                        onCreateBranch = { showNewBranchDialog = true },
                        onStashWithMessage = { showStashWithMessageDialog = true },
                        onOpenAnotherRepository = {
                            val repo = tabViewModel.openDirectoryPicker()

                            if (repo != null) {
                                tabViewModel.openAnotherRepository(repo, currentTabInformation)
                            }
                        },
                        onQuickActions = { showQuickActionsDialog = true },
                        onShowSettingsDialog = onShowSettingsDialog
                    )

                    RepoContent(
                        tabViewModel = tabViewModel,
                        diffSelected = diffSelected,
                        selectedItem = selectedItem,
                        repositoryState = repositoryState,
                        blameState = blameState,
                        showHistory = showHistory,
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.2f))
        )

        BottomInfoBar(tabViewModel)
    }
}

@Composable
private fun BottomInfoBar(tabViewModel: TabViewModel) {
    val userInfo by tabViewModel.authorInfoSimple.collectAsState()
    val newUpdate = tabViewModel.hasUpdates.collectAsState().value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .handMouseClickable { tabViewModel.showAuthorInfoDialog() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${userInfo.name ?: "Name not set"} <${userInfo.email ?: "Email not set"}>",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
            )
        }
        Spacer(Modifier.weight(1f, true))

        if (newUpdate != null) {
            SecondaryButton(
                text = "Update ${newUpdate.appVersion} available",
                onClick = { tabViewModel.openUrlInBrowser(newUpdate.downloadUrl) },
                backgroundButton = MaterialTheme.colors.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Text(
            "Version ${AppConstants.APP_VERSION}",
            style = MaterialTheme.typography.body2,
            maxLines = 1,
        )
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
    blameState: BlameState,
) {
    val rebaseInteractiveState by tabViewModel.rebaseInteractiveState.collectAsState()
    val density = LocalDensity.current.density

    var firstWidth by remember { mutableStateOf(180f) }
    var thirdWidth by remember { mutableStateOf(360f) }

    TripleVerticalSplitPanel(
        modifier = Modifier.fillMaxSize(),
        firstWidth = firstWidth,
        thirdWidth = thirdWidth,
        first = {
            SidePanel()
        },
        second = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (rebaseInteractiveState == RebaseInteractiveState.AwaitingInteraction && diffSelected == null) {
                    RebaseInteractive()
                } else if (blameState is BlameState.Loaded && !blameState.isMinimized) {
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
        },
        third = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                when (selectedItem) {
                    SelectedItem.UncommittedChanges -> {
                        UncommittedChanges(
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
                    }

                    is SelectedItem.CommitBasedItem -> {
                        CommitChanges(
                            selectedItem = selectedItem,
                            diffSelected = diffSelected,
                            onDiffSelected = { diffEntry ->
                                tabViewModel.minimizeBlame()
                                tabViewModel.newDiffSelected = DiffEntryType.CommitDiff(diffEntry)
                            },
                            onBlame = { tabViewModel.blameFile(it) },
                            onHistory = { tabViewModel.fileHistory(it) },
                        )
                    }

                    SelectedItem.None -> {}
                }
            }
        },
        onFirstSizeDrag = {
            val newWidth = firstWidth + it / density

            if (newWidth > 150) {
                firstWidth = newWidth
            }
        },
        onThirdSizeDrag = {
            val newWidth = thirdWidth - it / density

            if (newWidth > 150) {
                thirdWidth = newWidth
            }
        },
    )
}

sealed interface SelectedItem {
    data object None : SelectedItem
    data object UncommittedChanges : SelectedItem
    sealed class CommitBasedItem(val revCommit: RevCommit) : SelectedItem
    class Ref(val ref: org.eclipse.jgit.lib.Ref, revCommit: RevCommit) : CommitBasedItem(revCommit)
    class Commit(revCommit: RevCommit) : CommitBasedItem(revCommit)
    class Stash(revCommit: RevCommit) : CommitBasedItem(revCommit)
}