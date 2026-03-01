package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.jetpackduba.gitnuro.*
import com.jetpackduba.gitnuro.git.ProcessingState
import com.jetpackduba.gitnuro.ui.components.Notification
import com.jetpackduba.gitnuro.ui.dialogs.AddEditRemoteDialog
import com.jetpackduba.gitnuro.ui.dialogs.AddSubmodulesDialog
import com.jetpackduba.gitnuro.ui.dialogs.CloneDialog
import com.jetpackduba.gitnuro.ui.dialogs.CredentialsDialog
import com.jetpackduba.gitnuro.ui.dialogs.RenameBranchDialog
import com.jetpackduba.gitnuro.ui.dialogs.SetDefaultUpstreamBranchDialog
import com.jetpackduba.gitnuro.ui.dialogs.errors.ErrorDialog
import com.jetpackduba.gitnuro.ui.dialogs.settings.SettingsDialog
import com.jetpackduba.gitnuro.viewmodels.RepositorySelectionStatus
import com.jetpackduba.gitnuro.viewmodels.TabViewModel


fun <T : NavKey> NavBackStack<T>.addAndRemovePrevious(item: T) {
    this.add(item)

    repeat(lastIndex - 1) {
        this.removeFirst()
    }
}


@Composable
fun AppTab(
    tabViewModel: TabViewModel,
) {
    val errorManager = tabViewModel.errorsManager
    val lastError by errorManager.error.collectAsState(null)
    val notifications = errorManager.notification.collectAsState().value
        .toList()
        .sortedBy { it.first }
        .map { it.second }

    val repositorySelectionStatus = tabViewModel.repositorySelectionStatus.collectAsState()
    val repositorySelectionStatusValue = repositorySelectionStatus.value
    val processingState = tabViewModel.processing.collectAsState().value

    val backStack = tabViewModel.backStack
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }


    LaunchedEffect(tabViewModel) {
        // Init the tab content when the tab is selected and also remove the "initialPath" to avoid opening the
        // repository everytime the user changes between tabs
        val initialPath = tabViewModel.initialPath
        tabViewModel.initialPath = null

        if (initialPath != null) {
            tabViewModel.openRepository(initialPath)
        }
    }

    LaunchedEffect(lastError) {
        lastError?.let {
            backStack.add(Screen.Error(it))
        }
    }

    LaunchedEffect(repositorySelectionStatusValue) {
        val screen = when (repositorySelectionStatusValue) {
            RepositorySelectionStatus.None -> Screen.Welcome

            is RepositorySelectionStatus.Opening -> Screen.RepositoryLoading

            is RepositorySelectionStatus.Open -> Screen.RepositoryOpen
        }

        backStack.addAndRemovePrevious(screen)
    }


    Box {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
        ) {
            CredentialsDialog(tabViewModel)

            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategy = dialogStrategy,
                    entryProvider = entryProvider {
                        entry<Screen.Welcome> {
                            WelcomePage(
                                tabViewModel = tabViewModel,
                                onShowCloneDialog = { backStack.add(Screen.CloneRepository) },
                                onShowSettings = { backStack.add(Screen.Settings) }
                            )
                        }
                        entry<Screen.RepositoryLoading> {
                            val path = (repositorySelectionStatusValue as? RepositorySelectionStatus.Opening)?.path

                            if (path != null) {
                                LoadingRepository(path)
                            }

                        }
                        entry<Screen.RepositoryOpen> { key ->
                            val repositoryOpenViewModel =
                                (repositorySelectionStatusValue as? RepositorySelectionStatus.Open)?.viewModel
                            if (repositoryOpenViewModel != null) {
                                RepositoryOpenPage(
                                    repositoryOpenViewModel = repositoryOpenViewModel,
                                    onShowSettingsDialog = { backStack.add(Screen.Settings) },
                                    onShowCloneDialog = { backStack.add(Screen.CloneRepository) },
                                    onNavigate = { backStack.add(it) }
                                )
                            }
                        }
                        entry<Screen.Settings>(
                            metadata = DialogSceneStrategy.dialog()
                        ) {
                            SettingsDialog(
                                settingsViewModel = tabViewModel.tabViewModelsProvider.settingsViewModel,
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.CloneRepository>(
                            metadata = DialogSceneStrategy.dialog()
                        ) {
                            CloneDialog(
                                cloneViewModel = tabViewModel.tabViewModelsProvider.cloneViewModel,
                                onClose = { backStack.removeLastOrNull() },
                                onOpenRepository = { dir ->
                                    tabViewModel.openRepository(dir)
                                },
                            )
                        }
                        entry<Screen.BranchRename>(
                            metadata = DialogSceneStrategy.dialog()
                        ) {
                            RenameBranchDialog(
                                viewModel = tabViewModel.tabViewModelsProvider.renameBranchDialogViewModel,
                                branch = it.ref,
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.BranchChangeUpstream>(
                            metadata = DialogSceneStrategy.dialog()
                        ) {
                            SetDefaultUpstreamBranchDialog(
                                viewModel = tabViewModel.tabViewModelsProvider.setUpstreamBranchDialogViewModel,
                                branch = it.ref,
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.Error>(
                            metadata = DialogSceneStrategy.dialog()
                        ) {
                            ErrorDialog(
                                error = it.error,
                                onAccept = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.AddEditRemote>(
                            metadata = DialogSceneStrategy.dialog()
                        ) {
                            AddEditRemoteDialog(
                                remotesViewModel = tabViewModel.tabViewModelsProvider.sidePanelViewModel.remotesViewModel,
                                remoteWrapper = it.remote,
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.SubmoduleAdd>(
                            metadata = DialogSceneStrategy.dialog()
                        ) {
                            AddSubmodulesDialog(
                                viewModel = tabViewModel.tabViewModelsProvider.submoduleDialogViewModel,
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                    }
                )
            }
        }

        if (processingState is ProcessingState.Processing) {
            ProcessingScreen(
                processingState,
                onCancelOnGoingTask = { tabViewModel.cancelOngoingTask() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (notification in notifications) {
                Notification(notification)
            }
        }
    }
}
