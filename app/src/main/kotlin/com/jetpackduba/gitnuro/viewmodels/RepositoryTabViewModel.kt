package com.jetpackduba.gitnuro.viewmodels

import androidx.navigation3.runtime.NavBackStack
import com.jetpackduba.gitnuro.Screen
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.credentials.CredentialsState
import com.jetpackduba.gitnuro.domain.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.domain.interfaces.IFileChangesWatcher
import com.jetpackduba.gitnuro.domain.interfaces.IInitLocalRepositoryGitAction
import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.IErrorsRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.OpenRepositoryUseCase
import com.jetpackduba.gitnuro.domain.usecases.SetRepositorySelectionStateToNoneUseCase
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.system.OpenFilePickerGitAction
import com.jetpackduba.gitnuro.system.OpenUrlInBrowserGitAction
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.ui.IVerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.VerticalSplitPaneConfig
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.updates.UpdatesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val MIN_TIME_AFTER_GIT_OPERATION = 2000L

private const val TAG = "TabViewModel"

/**
 * Contains all the information related to a tab and its subcomponents (smaller composables like the log, branches,
 * commit changes, etc.). It holds a reference to every view model because this class lives as long as the tab is open (survives
 * across full app recompositions), therefore, tab's content can be recreated with these view models.
 */
class RepositoryTabViewModel @AssistedInject constructor(
    private val initLocalRepositoryGitAction: IInitLocalRepositoryGitAction,
    private val tabState: TabInstanceRepository,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: IFileChangesWatcher,
    private val credentialsStateManager: CredentialsStateManager,
    private val openFilePickerGitAction: OpenFilePickerGitAction,
    private val openUrlInBrowserGitAction: OpenUrlInBrowserGitAction,
    private val tabScope: TabCoroutineScope,
    private val verticalSplitPaneConfig: VerticalSplitPaneConfig,
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
    private val openRepositoryUseCase: OpenRepositoryUseCase,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val setRepositorySelectionStateToNoneUseCase: SetRepositorySelectionStateToNoneUseCase,
    @Assisted val initialPath: String?,
    updatesRepository: UpdatesRepository,
) : IVerticalSplitPaneConfig by verticalSplitPaneConfig,
    IGlobalMenuActionsViewModel by globalMenuActionsViewModel,
    TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(initialPath: String?): RepositoryTabViewModel
    }

    val errorsManager: IErrorsRepository = tabState.errorsRepository
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem

    val repositorySelectionState: StateFlow<RepositorySelectionState> = repositoryDataRepository.repositoryState

    val backStack = NavBackStack<Screen>(
        if (initialPath == null) {
            Screen.Welcome
        } else {
            Screen.RepositoryLoading
        }
    )

    val processingTask = repositoryStateRepository.currentTask
        .debounce(300L.milliseconds)
        .stateIn(
            scope = tabScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )


    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState


    fun openRepository(directory: String) {
        viewModelScope.launch {
            printLog(TAG, "Trying to open repository ${directory}")

            openRepositoryUseCase(directory)
        }
    }

    fun credentialsDenied() {
        credentialsStateManager.credentialsDenied()
    }

    fun httpCredentialsAccepted(user: String, password: String) {
        credentialsStateManager.httpCredentialsAccepted(user, password)
    }

    fun sshCredentialsAccepted(password: String) {
        credentialsStateManager.sshCredentialsAccepted(password)
    }

    fun gpgCredentialsAccepted(password: String) {
        credentialsStateManager.gpgCredentialsAccepted(password)
    }

    fun lfsCredentialsAccepted(user: String, password: String) {
        credentialsStateManager.lfsCredentialsAccepted(user, password)
    }

    var onRepositoryChanged: (path: String?) -> Unit = {}

    fun dispose() {
        fileChangesWatcher.close()
        tabScope.cancel()
    }

    fun openDirectoryPicker(): String? {
        val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath

        return openFilePickerGitAction(PickerType.DIRECTORIES, latestDirectoryOpened)
    }

    fun initLocalRepository(dir: String) = viewModelScope.launch {
        val repoDir = File(dir)
        initLocalRepositoryGitAction(repoDir)
        openRepository(dir)
    }

    val update: StateFlow<Update?> = updatesRepository.hasUpdatesFlow

    fun cancelOngoingTask() {
        tabState.cancelCurrentTask()
    }

    fun openUrlInBrowser(url: String) {
        openUrlInBrowserGitAction(url)
    }

    fun removeRepositoryFromRecent(repository: String) {
        appStateManager.removeRepositoryFromRecent(repository)
    }

    fun newTab() {
        setRepositorySelectionStateToNoneUseCase()
    }
}
