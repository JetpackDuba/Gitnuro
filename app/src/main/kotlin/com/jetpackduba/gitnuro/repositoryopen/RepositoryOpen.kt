package com.jetpackduba.gitnuro.repositoryopen

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
import com.jetpackduba.gitnuro.domain.models.Identity
import com.jetpackduba.gitnuro.domain.models.PullType
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.ui.*
import com.jetpackduba.gitnuro.ui.components.BottomInfoBar
import com.jetpackduba.gitnuro.ui.components.TripleVerticalSplitPanel
import com.jetpackduba.gitnuro.ui.diff.DiffPane
import com.jetpackduba.gitnuro.ui.log.Log
import com.jetpackduba.gitnuro.ui.status.StatusPane
import com.jetpackduba.gitnuro.updates.Update
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
                    viewModel = repositoryOpenViewModel,
                    modifier = Modifier
                        .padding(
                            vertical = 4.dp
                        )
                        .fillMaxWidth(),
                    onCreateBranch = { onNavigate(Screen.BranchCreate(null)) },
                    onStashWithMessage = { onNavigate(Screen.StashWithMessage) },
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
    userInfo: Identity,
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
            viewModel = repositoryOpenViewModel,
            selectedItem = selectedItem,
            repositoryState = repositoryState,
            blameState = blameState,
            onNavigate = onNavigate,
        )
    }
}

@Composable
fun MainContentView(
    viewModel: RepositoryOpenViewModel,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    blameState: BlameState,
    onNavigate: (Screen) -> Unit,
) {
    val diffSelected by viewModel.diffSelected.collectAsState()
    val rebaseInteractiveState by viewModel.rebaseInteractiveState.collectAsState()
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    val statusState by viewModel.statusState.collectAsState()

    // We create 2 mutableStates here because using directly the flow makes compose lose some drag events for some reason
    var firstWidth by remember(viewModel) { mutableStateOf(viewModel.firstPaneWidth.value) }
    var thirdWidth by remember(viewModel) { mutableStateOf(viewModel.thirdPaneWidth.value) }

    LaunchedEffect(Unit) {
        // Update the pane widths if they have been changed in a different tab
        viewModel.onPanelsWidthPersisted.collectLatest {
            firstWidth = viewModel.firstPaneWidth.value
            thirdWidth = viewModel.thirdPaneWidth.value
        }
    }

    TripleVerticalSplitPanel(
        modifier = Modifier.fillMaxSize(),
        firstWidth = if (rebaseInteractiveState is RebaseInteractiveState.AwaitingInteraction) 0f else firstWidth,
        thirdWidth = thirdWidth,
        first = {
            SidePanel(
                viewModel = viewModel,
                onNavigate = onNavigate,
            )
        },
        second = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (rebaseInteractiveState == RebaseInteractiveState.AwaitingInteraction /*&& diffSelected == null*/) {
                    RebaseInteractive(viewModel)
                } else if (blameState is BlameState.Loaded && !blameState.isMinimized) {
                    Blame(
                        filePath = blameState.filePath,
                        blameResult = blameState.blameResult,
                        onClose = { viewModel.resetBlameState() },
                        onSelectCommit = { viewModel.selectCommit(it) }
                    )
                } else {
                    Column {
                        Box(modifier = Modifier.weight(1f, true)) {
                            if (diffSelected?.entries?.count() == 1) {
                                val tabFocusRequester = LocalTabFocusRequester.current

                                DiffPane(
                                    viewModel = viewModel,
                                    onCloseDiffView = {
                                        tabFocusRequester.requestFocus()
                                    }
                                )
                            } else {
                                Log(
                                    viewModel = viewModel,
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
                                onExpand = { viewModel.expandBlame() },
                                onClose = { viewModel.resetBlameState() }
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
                val commitChangesState = viewModel.commitChangesState.collectAsState().value


                if (commitChangesState != null) {
                    CommitChanges(
                        viewModel = viewModel,
                        onBlame = { viewModel.blameFile(it) },
                        onHistory = { viewModel.fileHistory(it) },
                        commitChangesState = commitChangesState,
                    )
                } else {
                    StatusPane(
                        statusState = statusState,
                        onAction = { viewModel.onAction(it) },
                        onBlameFile = { viewModel.blameFile(it) },
                        onHistoryFile = { viewModel.fileHistory(it) }
                    )
                }
            }
        },
        onFirstSizeDragStarted = { currentWidth ->
            firstWidth = currentWidth
            viewModel.setFirstPaneWidth(currentWidth)
        },
        onFirstSizeChange = {
            val newWidth = firstWidth + it / density

            if (newWidth > 150 && rebaseInteractiveState !is RebaseInteractiveState.AwaitingInteraction) {
                firstWidth = newWidth
                viewModel.setFirstPaneWidth(newWidth)
            }
        },
        onFirstSizeDragStopped = {
            scope.launch {
                viewModel.persistFirstPaneWidth()
            }
        },
        onThirdSizeChange = {
            val newWidth = thirdWidth - it / density

            if (newWidth > 150) {
                thirdWidth = newWidth
                viewModel.setThirdPaneWidth(newWidth)
            }
        },
        onThirdSizeDragStarted = { currentWidth ->
            thirdWidth = currentWidth
            viewModel.setThirdPaneWidth(currentWidth)
        },
        onThirdSizeDragStopped = {
            scope.launch {
                viewModel.persistThirdPaneWidth()
            }
        },
    )
}

