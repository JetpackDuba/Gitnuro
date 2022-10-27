@file:OptIn(ExperimentalSplitPaneApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.ScrollableColumn
import com.jetpackduba.gitnuro.ui.dialogs.*
import com.jetpackduba.gitnuro.ui.diff.Diff
import com.jetpackduba.gitnuro.ui.log.Log
import com.jetpackduba.gitnuro.viewmodels.BlameState
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.SplitterScope
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.Cursor

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
                }
            },
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
                val rebaseInteractiveViewModel = tabViewModel.rebaseInteractiveViewModel

                if (repositoryState == RepositoryState.REBASING_INTERACTIVE && rebaseInteractiveViewModel != null) {
                    RebaseInteractive(rebaseInteractiveViewModel)
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Menu(
                            modifier = Modifier
                                .padding(
                                    vertical = 12.dp
                                )
                                .fillMaxWidth(),
                            onCreateBranch = { showNewBranchDialog = true },
                            onStashWithMessage = { showStashWithMessageDialog = true },
                            onGoToWorkspace = { tabViewModel.selectUncommitedChanges() },
                            onQuickActions = { showQuickActionsDialog = true }
                        )

                        RepoContent(
                            tabViewModel = tabViewModel,
                            diffSelected = diffSelected,
                            selectedItem = selectedItem,
                            repositoryState = repositoryState,
                            blameState = blameState,
                            showHistory = showHistory,
                            onShowSettingsDialog = onShowSettingsDialog
                        )
                    }
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
    onShowSettingsDialog: () -> Unit,
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
            onShowSettingsDialog,
        )
    }
}

@Composable
fun SidePanelOption(title: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
            .handMouseClickable(onClick)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colors.onBackground,
            modifier = Modifier
                .size(16.dp),
        )

        Text(
            text = title,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),
            maxLines = 1,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onBackground,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun MainContentView(
    tabViewModel: TabViewModel,
    diffSelected: DiffEntryType?,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
    onShowSettingsDialog: () -> Unit,

    ) {
    HorizontalSplitPane(
        splitPaneState = rememberSplitPaneState(initialPositionPercentage = 0.20f)
    ) {
        first(minSize = 180.dp) {
            Column {
                val state: ScrollState = rememberScrollState()

                val canBeScrolled by remember {
                    derivedStateOf {
                        state.maxValue > 0
                    }
                }

                ScrollableColumn(
                    state = state,
                    modifier = Modifier
                        .weight(1f),
                ) {
                    Branches()
                    Remotes()
                    Tags()
                    Stashes()
//                TODO: Enable on 1.2.0 when fully implemented Submodules()
                }

                Column {
                    if (canBeScrolled) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.2f))
                        )
                    }
                    SidePanelOption("Open another repository", "open.svg") { openRepositoryDialog(tabViewModel = tabViewModel) }
                    SidePanelOption("Refresh", "refresh.svg") { tabViewModel.refreshAll() }
                    SidePanelOption("Settings", "settings.svg", onShowSettingsDialog)
                }
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

                second(minSize = 250.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                    ) {
                        val safeSelectedItem = selectedItem
                        if (safeSelectedItem == SelectedItem.UncommitedChanges) {
                            UncommitedChanges(
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