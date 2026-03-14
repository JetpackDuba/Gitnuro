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
import com.jetpackduba.gitnuro.Screen
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.bottom_info_bar_email_not_set
import com.jetpackduba.gitnuro.app.generated.resources.bottom_info_bar_name_and_email
import com.jetpackduba.gitnuro.app.generated.resources.bottom_info_bar_name_not_set
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.models.PullType
import com.jetpackduba.gitnuro.domain.models.AuthorInfoSimple
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.ui.components.BottomInfoBar
import com.jetpackduba.gitnuro.ui.components.TripleVerticalSplitPanel
import com.jetpackduba.gitnuro.ui.dialogs.AuthorDialog
import com.jetpackduba.gitnuro.ui.dialogs.SignOffDialog
import com.jetpackduba.gitnuro.ui.dialogs.StashWithMessageDialog
import com.jetpackduba.gitnuro.ui.diff.DiffPane
import com.jetpackduba.gitnuro.ui.log.Log
import com.jetpackduba.gitnuro.ui.status.StatusPane
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.viewmodels.BlameState
import com.jetpackduba.gitnuro.viewmodels.RepositoryOpenViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.RepositoryState
import org.jetbrains.compose.resources.stringResource

@Composable
fun RepositoryOpenPage(
    repositoryOpenViewModel: RepositoryOpenViewModel,
    onNavigate: (Screen) -> Unit, // TODO Perhaps have specific callbacks instead of directly navigating
) {
    val repositoryState by repositoryOpenViewModel.repositoryState.collectAsState()
    val selectedItem by repositoryOpenViewModel.selectedItem.collectAsState()
    val blameState by repositoryOpenViewModel.blameState.collectAsState()
    val showHistory by repositoryOpenViewModel.showHistory.collectAsState()

    var showStashWithMessageDialog by remember { mutableStateOf(false) }

    if (showStashWithMessageDialog) {
        StashWithMessageDialog(
            onDismiss = {
                showStashWithMessageDialog = false
            },
            onAccept = { stashMessage ->
                repositoryOpenViewModel.stashWithMessage(stashMessage)
                showStashWithMessageDialog = false
            }
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
                        onNavigate(Screen.BranchCreate(null))
                        true
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
                        onNavigate(Screen.Settings)
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
                    onCreateBranch = { onNavigate(Screen.BranchCreate(null)) },
                    onStashWithMessage = { showStashWithMessageDialog = true },
                    onOpenAnotherRepository = { repositoryOpenViewModel.openAnotherRepository(it) },
                    onOpenAnotherRepositoryFromPicker = {
                        val repoToOpen = repositoryOpenViewModel.openDirectoryPicker()

                        if (repoToOpen != null) {
                            repositoryOpenViewModel.openAnotherRepository(repoToOpen)
                        }
                    },
                    onQuickActions = { onNavigate(Screen.QuickActions) },
                    onShowSettingsDialog = { onNavigate(Screen.Settings) },
                    showOpenPopup = showOpenPopup,
                    onShowOpenPopupChange = { showOpenPopup = it }
                )

                RepoContent(
                    repositoryOpenViewModel = repositoryOpenViewModel,
                    selectedItem = selectedItem,
                    repositoryState = repositoryState,
                    blameState = blameState,
                    showHistory = showHistory,
                    onNavigate = onNavigate,
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
            onShowAuthorInfoDialog = { onNavigate(Screen.Author) },
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
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
    showHistory: Boolean,
    onNavigate: (Screen) -> Unit,
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
            repositoryOpenViewModel = repositoryOpenViewModel,
            selectedItem = selectedItem,
            repositoryState = repositoryState,
            blameState = blameState,
            onNavigate = onNavigate,
        )
    }
}

@Composable
fun MainContentView(
    repositoryOpenViewModel: RepositoryOpenViewModel,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
    onNavigate: (Screen) -> Unit,
) {
    val diffSelected by repositoryOpenViewModel.diffSelected.collectAsState()
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
                onNavigate = onNavigate,
            )
        },
        second = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (rebaseInteractiveState == RebaseInteractiveState.AwaitingInteraction /*&& diffSelected == null*/) {
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
                            if (diffSelected?.entries?.count() == 1) {
                                val diffViewModel = repositoryOpenViewModel.diffViewModel
                                val tabFocusRequester = LocalTabFocusRequester.current

                                DiffPane(
                                    diffViewModel = diffViewModel,
                                    onCloseDiffView = {
                                        tabFocusRequester.requestFocus()
                                    }
                                )
                            } else {
                                Log(
                                    logViewModel = repositoryOpenViewModel.tabViewModelsProvider.logViewModel,
                                    selectedItem = selectedItem,
                                    repositoryState = repositoryState,
                                    // TODO Move nav outside of this? Applies to next lines
                                    onCreateBranch = { onNavigate(Screen.BranchCreate(it)) },
                                    onResetBranch = { onNavigate(Screen.BranchReset(it)) },
                                    onCreateTag = { onNavigate(Screen.TagCreate(it)) },
                                    onChangeUpstreamBranch = { onNavigate(Screen.BranchChangeUpstream(it)) },
                                    onRenameBranch = { onNavigate(Screen.BranchRename(it)) },
                                )
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
                        StatusPane(
                            statusPaneViewModel = repositoryOpenViewModel.tabViewModelsProvider.statusPaneViewModel,
                            repositoryState = repositoryState,
                            onBlameFile = { repositoryOpenViewModel.blameFile(it) },
                            onHistoryFile = { repositoryOpenViewModel.fileHistory(it) }
                        )
                    }

                    is SelectedItem.CommitBasedItem -> {
                        CommitChanges(
                            commitChangesViewModel = repositoryOpenViewModel.tabViewModelsProvider.commitChangesViewModel,
                            selectedItem = selectedItem,
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

