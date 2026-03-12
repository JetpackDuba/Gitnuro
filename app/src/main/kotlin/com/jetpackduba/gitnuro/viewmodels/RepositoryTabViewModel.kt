package com.jetpackduba.gitnuro.viewmodels

import androidx.navigation3.runtime.NavBackStack
import com.jetpackduba.gitnuro.Screen
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.di.AppComponent
import com.jetpackduba.gitnuro.domain.credentials.CredentialsState
import com.jetpackduba.gitnuro.domain.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.domain.interfaces.IFileChangesWatcher
import com.jetpackduba.gitnuro.domain.interfaces.IInitLocalRepositoryGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IOpenRepositoryGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IOpenSubmoduleRepositoryGitAction
import com.jetpackduba.gitnuro.domain.models.ProcessingState
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.newErrorNow
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.IErrorsRepository
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

private const val MIN_TIME_AFTER_GIT_OPERATION = 2000L

private const val TAG = "TabViewModel"

/**
 * Contains all the information related to a tab and its subcomponents (smaller composables like the log, branches,
 * commit changes, etc.). It holds a reference to every view model because this class lives as long as the tab is open (survives
 * across full app recompositions), therefore, tab's content can be recreated with these view models.
 */
class RepositoryTabViewModel @AssistedInject constructor(
    private val initLocalRepositoryGitAction: IInitLocalRepositoryGitAction,
    private val openRepositoryGitAction: IOpenRepositoryGitAction,
    private val openSubmoduleRepositoryGitAction: IOpenSubmoduleRepositoryGitAction,
    private val tabState: TabInstanceRepository,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: IFileChangesWatcher,
    private val credentialsStateManager: CredentialsStateManager,
    private val openFilePickerGitAction: OpenFilePickerGitAction,
    private val openUrlInBrowserGitAction: OpenUrlInBrowserGitAction,
    private val tabScope: CoroutineScope,
    private val verticalSplitPaneConfig: VerticalSplitPaneConfig,
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
    private val repositoryOpenViewModelProvider: Provider<RepositoryOpenViewModel>,
    @Assisted val initialPath: String?,
    updatesRepository: UpdatesRepository,
) : IVerticalSplitPaneConfig by verticalSplitPaneConfig,
    IGlobalMenuActionsViewModel by globalMenuActionsViewModel {
    @AssistedFactory
    interface Factory {
        fun create(initialPath: String?): RepositoryTabViewModel
    }

    //var initialPath: String? = null // Stores the path that should be opened when the tab is selected
    val errorsManager: IErrorsRepository = tabState.errorsRepository
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem

    val backStack = NavBackStack<Screen>(Screen.Welcome)

    private val _repositorySelectionStatus = MutableStateFlow<RepositorySelectionStatus>(RepositorySelectionStatus.None)
    val repositorySelectionStatus: StateFlow<RepositorySelectionStatus>
        get() = _repositorySelectionStatus

    val processing: StateFlow<ProcessingState> = tabState.processing
        .debounce(300L)
        .stateIn(
            scope = tabScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ProcessingState.None
        )

    private var hasRepositoryAlreadyOpened: Boolean = false

    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState

    fun openRepository(directory: String) {
        if (!hasRepositoryAlreadyOpened) {
            openRepository(File(directory))
            hasRepositoryAlreadyOpened = true
        }
    }

    fun openRepository(directory: File) = tabState.safeProcessingWithoutGit {
        printLog(TAG, "Trying to open repository ${directory.absoluteFile}")

        _repositorySelectionStatus.value = RepositorySelectionStatus.Opening(directory.absolutePath)

        try {
            val repository: Repository = if (directory.listFiles()?.any { it.name == ".git" && it.isFile } == true) {
                openSubmoduleRepositoryGitAction(directory)
            } else {
                openRepositoryGitAction(directory)
            }

            repository.workTree // test if repository is valid

            val path = if (directory.name == ".git") {
                directory.parent
            } else
                directory.absolutePath

            onRepositoryChanged(path)

            val git = Git(repository)
            tabState.initGit(git)

            _repositorySelectionStatus.value = RepositorySelectionStatus.Open(repositoryOpenViewModelProvider.get())

            tabState.refreshData(RefreshType.ALL_DATA)
        } catch (ex: Exception) {
            onRepositoryChanged(null)
            ex.printStackTrace()

            errorsManager.addError(
                newErrorNow(
                    taskType = TaskType.REPOSITORY_OPEN,
                    exception = ex
                )
            )
            _repositorySelectionStatus.value = RepositorySelectionStatus.None
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

    fun initLocalRepository(dir: String) = tabState.safeProcessingWithoutGit {
        val repoDir = File(dir)
        initLocalRepositoryGitAction(repoDir)
        openRepository(repoDir)
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
}


sealed class RepositorySelectionStatus {
    data object None : RepositorySelectionStatus()
    data class Opening(val path: String) : RepositorySelectionStatus()
    data class Open(val viewModel: RepositoryOpenViewModel) : RepositorySelectionStatus()
}