package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.jetpackduba.gitnuro.LoadingRepository
import com.jetpackduba.gitnuro.ProcessingScreen
import com.jetpackduba.gitnuro.Screen
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.lfs
import com.jetpackduba.gitnuro.domain.credentials.CredentialsRequest
import com.jetpackduba.gitnuro.domain.models.ProcessingState
import com.jetpackduba.gitnuro.tabViewModel
import com.jetpackduba.gitnuro.theme.dialogOverlay
import com.jetpackduba.gitnuro.ui.components.Notification
import com.jetpackduba.gitnuro.ui.dialogs.*
import com.jetpackduba.gitnuro.ui.dialogs.base.UserPasswordDialog
import com.jetpackduba.gitnuro.ui.dialogs.errors.ErrorDialog
import com.jetpackduba.gitnuro.ui.dialogs.settings.SettingsDialog
import com.jetpackduba.gitnuro.viewmodels.RepositorySelectionStatus
import com.jetpackduba.gitnuro.viewmodels.RepositoryTabViewModel
import org.jetbrains.compose.resources.painterResource


fun <T : NavKey> NavBackStack<T>.addAndRemovePrevious(item: T) {
    this.add(item)

    repeat(lastIndex - 1) {
        this.removeFirst()
    }
}


@Composable
fun AppTab(
    repositoryTabViewModel: RepositoryTabViewModel,
) {
    val errorManager = repositoryTabViewModel.errorsManager
    val lastError by errorManager.error.collectAsState(null)
    val notifications = errorManager.notification.collectAsState().value
        .toList()
        .sortedBy { it.first }
        .map { it.second }

    val repositorySelectionStatus = repositoryTabViewModel.repositorySelectionStatus.collectAsState()
    val repositorySelectionStatusValue = repositorySelectionStatus.value
    val processingState = repositoryTabViewModel.processing.collectAsState().value

    val backStack = repositoryTabViewModel.backStack
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }


    LaunchedEffect(repositoryTabViewModel) {
        // Init the tab content when the tab is selected and also remove the "initialPath" to avoid opening the
        // repository everytime the user changes between tabs
        val initialPath = repositoryTabViewModel.initialPath

        if (initialPath != null) {
            repositoryTabViewModel.openRepository(initialPath)
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

        if (!backStack.contains(screen)) {
            backStack.addAndRemovePrevious(screen)
        }
    }

    val dialogsMetadata =
        DialogSceneStrategy.dialog(
            dialogProperties = DialogProperties(
                scrimColor = MaterialTheme.colors.dialogOverlay,
                dismissOnClickOutside = false,
            )
        )

    val credentialsState by repositoryTabViewModel.credentialsState.collectAsState()

    LaunchedEffect(credentialsState) {
        val destination = when (val state = credentialsState) {
            is CredentialsRequest.GpgCredentialsRequest -> Screen.GpgCredentials(state)
            CredentialsRequest.HttpCredentialsRequest -> Screen.HttpCredentials
            CredentialsRequest.LfsCredentialsRequest -> Screen.LfsCredentials
            CredentialsRequest.SshCredentialsRequest -> Screen.SshCredentials
            else -> null
        }

        if (destination != null) {
            backStack.add(destination)
        }
    }

    Box {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
        ) {

            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategy = dialogStrategy,
                    entryProvider = entryProvider {
                        entry<Screen.Welcome> {
                            WelcomePage(
                                repositoryTabViewModel = repositoryTabViewModel,
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
                        entry<Screen.RepositoryOpen> { entry ->
                            val viewModel = tabViewModel(entry) { it.repositoryOpenViewModel }

                            RepositoryOpenPage(
                                repositoryOpenViewModel = viewModel,
                                onShowSettingsDialog = { backStack.add(Screen.Settings) },
                                onShowCloneDialog = { backStack.add(Screen.CloneRepository) },
                                onNavigate = { backStack.add(it) }
                            )
                        }
                        entry<Screen.Settings>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            val viewModel = tabViewModel(entry, { it.settingsViewModel })
                            SettingsDialog(
                                settingsViewModel = viewModel,
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.CloneRepository>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            CloneDialog(
                                cloneViewModel = tabViewModel(entry) { it.cloneViewModel },
                                onClose = { backStack.removeLastOrNull() },
                                onOpenRepository = { dir ->
                                    repositoryTabViewModel.openRepository(dir)
                                },
                            )
                        }
                        entry<Screen.BranchRename>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            RenameBranchDialog(
                                viewModel = tabViewModel(entry) { viewModelsProvider ->
                                    viewModelsProvider
                                        .renameBranchDialogViewModelFactory
                                        .create(entry.ref)
                                },
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.BranchCreate>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            CreateBranchDialog(
                                viewModel = tabViewModel(entry) { it.createBranchViewModelFactory.create(entry.targetCommit) },
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.BranchChangeUpstream>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            SetDefaultUpstreamBranchDialog(
                                viewModel = tabViewModel(entry) { viewModelsProvider ->
                                    viewModelsProvider
                                        .setUpstreamBranchDialogViewModelFactory
                                        .create(entry.ref)
                                },
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.Error>(
                            metadata = dialogsMetadata
                        ) {
                            ErrorDialog(
                                error = it.error,
                                onAccept = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.AddEditRemote>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            AddEditRemoteDialog(
                                viewModel = tabViewModel(entry) { viewModelsProvider ->
                                    viewModelsProvider
                                        .addEditRemoteViewModelFactory
                                        .create(entry.remote)
                                },
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.SubmoduleAdd>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            AddSubmodulesDialog(
                                viewModel = tabViewModel(entry) { it.submoduleDialogViewModel },
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.HttpCredentials>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            HttpCredentialsDialog(
                                onDismiss = {
                                    repositoryTabViewModel.credentialsDenied()
                                    backStack.removeLastOrNull()
                                },
                                onAccept = { user, password ->
                                    repositoryTabViewModel.httpCredentialsAccepted(user, password)
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                        entry<Screen.SshCredentials>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            SshPasswordDialog(
                                onReject = {
                                    repositoryTabViewModel.credentialsDenied()
                                    backStack.removeLastOrNull()
                                },
                                onAccept = { password ->
                                    repositoryTabViewModel.sshCredentialsAccepted(password)
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                        entry<Screen.GpgCredentials>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            GpgPasswordDialog(
                                gpgCredentialsRequest = entry.credentialsRequest,
                                onReject = {
                                    repositoryTabViewModel.credentialsDenied()
                                    backStack.removeLastOrNull()
                                },
                                onAccept = { password ->
                                    repositoryTabViewModel.gpgCredentialsAccepted(password)
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                        entry<Screen.LfsCredentials>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            // TODO Refactor dialogs to have their own view models and not rely on repositoryTabViewModel
                            UserPasswordDialog(
                                title = "LFS Server Credentials",
                                subtitle = "Introduce the credentials for your LFS server",
                                icon = painterResource(Res.drawable.lfs),
                                onDismiss = {
                                    repositoryTabViewModel.credentialsDenied()
                                    backStack.removeLastOrNull()
                                },
                                onAccept = { user, password ->
                                    repositoryTabViewModel.lfsCredentialsAccepted(user, password)
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                        entry<Screen.SignOffData>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            SignOffDialog(
                                viewModel = tabViewModel(entry) { it.signOffDialogViewModel },
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.TagCreate>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            CreateTagDialog(
                                viewModel = tabViewModel(entry) { viewModelsProvider ->
                                    viewModelsProvider.createTagViewModelFactory.create(entry.targetCommit)
                                },
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.BranchReset>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            ResetBranchDialog(
                                viewModel = tabViewModel(entry) { viewModelsProvider ->
                                    viewModelsProvider.resetBranchViewModelFactory.create(entry.targetCommit)
                                },
                                onDismiss = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Screen.QuickActions>(
                            metadata = dialogsMetadata
                        ) { entry ->
                            QuickActionsDialog(
                                viewModel = tabViewModel(entry) { it.quickActionsViewModel },
                                onDismiss = { backStack.removeLastOrNull() },
                                onShowSignOff = {
                                    backStack.removeLastOrNull()
                                    backStack.add(Screen.SignOffData)
                                },
                                onShowClone = {
                                    backStack.removeLastOrNull()
                                    backStack.add(Screen.Clone)
                                },
                            )
                        }
                    }
                )
            }
        }

        if (processingState is ProcessingState.Processing) {
            ProcessingScreen(
                processingState,
                onCancelOnGoingTask = { repositoryTabViewModel.cancelOngoingTask() }
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
