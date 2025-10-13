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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.LocalTabFocusRequester
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.bottom_info_bar_email_not_set
import com.jetpackduba.gitnuro.generated.resources.bottom_info_bar_name_and_email
import com.jetpackduba.gitnuro.generated.resources.bottom_info_bar_name_not_set
import com.jetpackduba.gitnuro.git.DiffType
import com.jetpackduba.gitnuro.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.git.remote_operations.PullType
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.models.AuthorInfoSimple
import com.jetpackduba.gitnuro.ui.components.BottomInfoBar
import com.jetpackduba.gitnuro.ui.components.TripleVerticalSplitPanel
import com.jetpackduba.gitnuro.ui.dialogs.*
import com.jetpackduba.gitnuro.ui.diff.Diff
import com.jetpackduba.gitnuro.ui.log.Log
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.viewmodels.BlameState
import com.jetpackduba.gitnuro.viewmodels.RepositoryOpenViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.resources.stringResource

@Composable
fun RepositoryOpenPage(
    repositoryOpenViewModel: RepositoryOpenViewModel,
    onShowSettingsDialog: () -> Unit,
    onShowCloneDialog: () -> Unit,
) {
    val repositoryState by repositoryOpenViewModel.repositoryState.collectAsState()
    val diffSelected by repositoryOpenViewModel.diffSelected.collectAsState()
    val selectedItem by repositoryOpenViewModel.selectedItem.collectAsState()
    val blameState by repositoryOpenViewModel.blameState.collectAsState()
    val showHistory by repositoryOpenViewModel.showHistory.collectAsState()
    val showAuthorInfo by repositoryOpenViewModel.showAuthorInfo.collectAsState()

    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showStashWithMessageDialog by remember { mutableStateOf(false) }
    var showQuickActionsDialog by remember { mutableStateOf(false) }
    var showSignOffDialog by remember { mutableStateOf(false) }

    if (showNewBranchDialog) {
        NewBranchDialog(
            onDismiss = {
                showNewBranchDialog = false
            },
            onAccept = { branchName ->
                repositoryOpenViewModel.createBranch(branchName)
                showNewBranchDialog = false
            }
        )
    } else if (showStashWithMessageDialog) {
        StashWithMessageDialog(
            onDismiss = {
                showStashWithMessageDialog = false
            },
            onAccept = { stashMessage ->
                repositoryOpenViewModel.stashWithMessage(stashMessage)
                showStashWithMessageDialog = false
            }
        )
    } else if (showAuthorInfo) {
        val authorViewModel = repositoryOpenViewModel.authorViewModel
        if (authorViewModel != null) {
            AuthorDialog(
                authorViewModel = authorViewModel,
                onClose = {
                    repositoryOpenViewModel.closeAuthorInfoDialog()
                }
            )
        }
    } else if (showQuickActionsDialog) {
        QuickActionsDialog(
            onClose = { showQuickActionsDialog = false },
            onAction = {
                showQuickActionsDialog = false
                when (it) {
                    QuickActionType.OPEN_DIR_IN_FILE_MANAGER -> repositoryOpenViewModel.openFolderInFileExplorer()
                    QuickActionType.OPEN_DIR_IN_EDITOR -> repositoryOpenViewModel.openFolderInEditor()
                    QuickActionType.CLONE -> onShowCloneDialog()
                    QuickActionType.REFRESH -> repositoryOpenViewModel.refreshAll()
                    QuickActionType.SIGN_OFF -> showSignOffDialog = true
                }
            },
        )
    } else if (showSignOffDialog) {
        SignOffDialog(
            viewModel = repositoryOpenViewModel.tabViewModelsProvider.signOffDialogViewModel,
            onDismiss = { showSignOffDialog = false },
        )
    }

    val focusRequester = remember { FocusRequester() }
    var showOpenPopup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable(true)
            .onPreviewKeyEvent {
                when {
                    it.matchesBinding(KeybindingOption.PULL) -> {
                        repositoryOpenViewModel.pull(PullType.DEFAULT)
                        true
                    }

                    it.matchesBinding(KeybindingOption.PUSH) -> {
                        repositoryOpenViewModel.push()
                        true
                    }

                    it.matchesBinding(KeybindingOption.BRANCH_CREATE) -> {
                        if (!showNewBranchDialog) {
                            showNewBranchDialog = true
                            true
                        } else {
                            false
                        }
                    }

                    it.matchesBinding(KeybindingOption.STASH) -> {
                        repositoryOpenViewModel.stash()
                        true
                    }

                    it.matchesBinding(KeybindingOption.STASH_POP) -> {
                        repositoryOpenViewModel.popStash()
                        true
                    }

                    it.matchesBinding(KeybindingOption.EXIT) -> {
                        repositoryOpenViewModel.closeLastView()
                        true
                    }

                    it.matchesBinding(KeybindingOption.REFRESH) -> {
                        repositoryOpenViewModel.refreshAll()
                        true
                    }

                    it.matchesBinding(KeybindingOption.OPEN_REPOSITORY) -> {
                        showOpenPopup = true
                        true
                    }

                    it.matchesBinding(KeybindingOption.SETTINGS) -> {
                        onShowSettingsDialog()
                        true
                    }

                    else -> false
                }

            }
    ) {
        CompositionLocalProvider(
            LocalTabFocusRequester provides focusRequester
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Menu(
                    menuViewModel = repositoryOpenViewModel.tabViewModelsProvider.menuViewModel,
                    modifier = Modifier
                        .padding(
                            vertical = 4.dp
                        )
                        .fillMaxWidth(),
                    onCreateBranch = { showNewBranchDialog = true },
                    onStashWithMessage = { showStashWithMessageDialog = true },
                    onOpenAnotherRepository = { repositoryOpenViewModel.openAnotherRepository(it) },
                    onOpenAnotherRepositoryFromPicker = {
                        val repoToOpen = repositoryOpenViewModel.openDirectoryPicker()

                        if (repoToOpen != null) {
                            repositoryOpenViewModel.openAnotherRepository(repoToOpen)
                        }
                    },
                    onQuickActions = { showQuickActionsDialog = true },
                    onShowSettingsDialog = onShowSettingsDialog,
                    showOpenPopup = showOpenPopup,
                    onShowOpenPopupChange = { showOpenPopup = it }
                )

                RepoContent(
                    repositoryOpenViewModel = repositoryOpenViewModel,
                    diffSelected = diffSelected,
                    selectedItem = selectedItem,
                    repositoryState = repositoryState,
                    blameState = blameState,
                    showHistory = showHistory,
                )
            }
        }

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.2f))
        )


        val userInfo by repositoryOpenViewModel.authorInfoSimple.collectAsState()
        val newUpdate = repositoryOpenViewModel.update.collectAsState().value

        RepositoryOpenBottomInfoBar(
            userInfo,
            newUpdate,
            onOpenUrlInBrowser = { repositoryOpenViewModel.openUrlInBrowser(it) },
            onShowAuthorInfoDialog = { repositoryOpenViewModel.showAuthorInfoDialog() },
        )
    }

    LaunchedEffect(repositoryOpenViewModel) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun RepositoryOpenBottomInfoBar(
    userInfo: AuthorInfoSimple,
    newUpdate: Update?,
    onOpenUrlInBrowser: (String) -> Unit,
    onShowAuthorInfoDialog: () -> Unit,
) {
    BottomInfoBar(
        newUpdate,
        onOpenUrlInBrowser,
        leadingContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .handMouseClickable { onShowAuthorInfoDialog() },
                contentAlignment = Alignment.Center,
            ) {
                val name = userInfo.name ?: stringResource(Res.string.bottom_info_bar_name_not_set)
                val email = userInfo.email ?: stringResource(Res.string.bottom_info_bar_email_not_set)

                Text(
                    text = stringResource(Res.string.bottom_info_bar_name_and_email, name, email),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }
    )
}

@Composable
fun RepoContent(
    repositoryOpenViewModel: RepositoryOpenViewModel,
    diffSelected: DiffType?,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
    showHistory: Boolean,
) {
    if (showHistory) {
        val historyViewModel = repositoryOpenViewModel.historyViewModel

        if (historyViewModel != null) {
            FileHistory(
                historyViewModel = historyViewModel,
                onClose = {
                    repositoryOpenViewModel.closeHistory()
                }
            )
        }
    } else {
        MainContentView(
            repositoryOpenViewModel,
            diffSelected,
            selectedItem,
            repositoryState,
            blameState,
        )
    }
}

@Composable
fun MainContentView(
    repositoryOpenViewModel: RepositoryOpenViewModel,
    diffSelected: DiffType?,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
) {
    val rebaseInteractiveState by repositoryOpenViewModel.rebaseInteractiveState.collectAsState()
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    // We create 2 mutableStates here because using directly the flow makes compose lose some drag events for some reason
    var firstWidth by remember(repositoryOpenViewModel) { mutableStateOf(repositoryOpenViewModel.firstPaneWidth.value) }
    var thirdWidth by remember(repositoryOpenViewModel) { mutableStateOf(repositoryOpenViewModel.thirdPaneWidth.value) }

    LaunchedEffect(Unit) {
        // Update the pane widths if they have been changed in a different tab
        repositoryOpenViewModel.onPanelsWidthPersisted.collectLatest {
            firstWidth = repositoryOpenViewModel.firstPaneWidth.value
            thirdWidth = repositoryOpenViewModel.thirdPaneWidth.value
        }
    }

    TripleVerticalSplitPanel(
        modifier = Modifier.fillMaxSize(),
        firstWidth = if (rebaseInteractiveState is RebaseInteractiveState.AwaitingInteraction) 0f else firstWidth,
        thirdWidth = thirdWidth,
        first = {
            SidePanel(
                sidePanelViewModel = repositoryOpenViewModel.tabViewModelsProvider.sidePanelViewModel,
                viewModelsProvider = repositoryOpenViewModel.tabViewModelsProvider,
            )
        },
        second = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (rebaseInteractiveState == RebaseInteractiveState.AwaitingInteraction && diffSelected == null) {
                    RebaseInteractive(repositoryOpenViewModel.tabViewModelsProvider.rebaseInteractiveViewModel)
                } else if (blameState is BlameState.Loaded && !blameState.isMinimized) {
                    Blame(
                        filePath = blameState.filePath,
                        blameResult = blameState.blameResult,
                        onClose = { repositoryOpenViewModel.resetBlameState() },
                        onSelectCommit = { repositoryOpenViewModel.selectCommit(it) }
                    )
                } else {
                    Column {
                        Box(modifier = Modifier.weight(1f, true)) {
                            when (diffSelected) {
                                null -> {
                                    Log(
                                        logViewModel = repositoryOpenViewModel.tabViewModelsProvider.logViewModel,
                                        selectedItem = selectedItem,
                                        repositoryState = repositoryState,
                                        viewModelsProvider = repositoryOpenViewModel.tabViewModelsProvider,
                                    )
                                }

                                else -> {
                                    val diffViewModel = repositoryOpenViewModel.diffViewModel

                                    if (diffViewModel != null) {
                                        val tabFocusRequester = LocalTabFocusRequester.current
                                        Diff(
                                            diffViewModel = diffViewModel,
                                            onCloseDiffView = {
                                                repositoryOpenViewModel.newDiffSelected = null
                                                tabFocusRequester.requestFocus()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (blameState is BlameState.Loaded) { // BlameState.isMinimized is true here
                            MinimizedBlame(
                                filePath = blameState.filePath,
                                onExpand = { repositoryOpenViewModel.expandBlame() },
                                onClose = { repositoryOpenViewModel.resetBlameState() }
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
                            statusViewModel = repositoryOpenViewModel.tabViewModelsProvider.statusViewModel,
                            selectedEntryType = diffSelected,
                            repositoryState = repositoryState,
                            onStagedDiffEntrySelected = { diffEntry ->
                                repositoryOpenViewModel.minimizeBlame()

                                repositoryOpenViewModel.newDiffSelected = if (diffEntry != null) {
                                    if (repositoryState == RepositoryState.SAFE)
                                        DiffType.SafeStagedDiff(diffEntry)
                                    else
                                        DiffType.UnsafeStagedDiff(diffEntry)
                                } else {
                                    null
                                }
                            },
                            onUnstagedDiffEntrySelected = { diffEntry ->
                                repositoryOpenViewModel.minimizeBlame()

                                if (repositoryState == RepositoryState.SAFE)
                                    repositoryOpenViewModel.newDiffSelected = DiffType.SafeUnstagedDiff(diffEntry)
                                else
                                    repositoryOpenViewModel.newDiffSelected = DiffType.UnsafeUnstagedDiff(diffEntry)
                            },
                            onBlameFile = { repositoryOpenViewModel.blameFile(it) },
                            onHistoryFile = { repositoryOpenViewModel.fileHistory(it) }
                        )
                    }

                    is SelectedItem.CommitBasedItem -> {
                        CommitChanges(
                            commitChangesViewModel = repositoryOpenViewModel.tabViewModelsProvider.commitChangesViewModel,
                            selectedItem = selectedItem,
                            diffSelected = diffSelected,
                            onDiffSelected = { diffEntry ->
                                repositoryOpenViewModel.minimizeBlame()
                                repositoryOpenViewModel.newDiffSelected = DiffType.CommitDiff(diffEntry)
                            },
                            onBlame = { repositoryOpenViewModel.blameFile(it) },
                            onHistory = { repositoryOpenViewModel.fileHistory(it) },
                        )
                    }

                    SelectedItem.None -> {}
                }
            }
        },
        onFirstSizeDragStarted = { currentWidth ->
            firstWidth = currentWidth
            repositoryOpenViewModel.setFirstPaneWidth(currentWidth)
        },
        onFirstSizeChange = {
            val newWidth = firstWidth + it / density

            if (newWidth > 150 && rebaseInteractiveState !is RebaseInteractiveState.AwaitingInteraction) {
                firstWidth = newWidth
                repositoryOpenViewModel.setFirstPaneWidth(newWidth)
            }
        },
        onFirstSizeDragStopped = {
            scope.launch {
                repositoryOpenViewModel.persistFirstPaneWidth()
            }
        },
        onThirdSizeChange = {
            val newWidth = thirdWidth - it / density

            if (newWidth > 150) {
                thirdWidth = newWidth
                repositoryOpenViewModel.setThirdPaneWidth(newWidth)
            }
        },
        onThirdSizeDragStarted = { currentWidth ->
            thirdWidth = currentWidth
            repositoryOpenViewModel.setThirdPaneWidth(currentWidth)
        },
        onThirdSizeDragStopped = {
            scope.launch {
                repositoryOpenViewModel.persistThirdPaneWidth()
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