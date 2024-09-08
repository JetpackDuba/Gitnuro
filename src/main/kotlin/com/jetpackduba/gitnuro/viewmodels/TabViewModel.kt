package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsState
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.exceptions.WatcherInitException
import com.jetpackduba.gitnuro.git.*
import com.jetpackduba.gitnuro.git.branches.CreateBranchUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.git.repository.InitLocalRepositoryUseCase
import com.jetpackduba.gitnuro.git.repository.OpenRepositoryUseCase
import com.jetpackduba.gitnuro.git.repository.OpenSubmoduleRepositoryUseCase
import com.jetpackduba.gitnuro.git.stash.StashChangesUseCase
import com.jetpackduba.gitnuro.git.workspace.StageUntrackedFileUseCase
import com.jetpackduba.gitnuro.logging.printDebug
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.managers.ErrorsManager
import com.jetpackduba.gitnuro.managers.newErrorNow
import com.jetpackduba.gitnuro.models.AuthorInfoSimple
import com.jetpackduba.gitnuro.models.errorNotification
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.system.OpenFilePickerUseCase
import com.jetpackduba.gitnuro.system.OpenUrlInBrowserUseCase
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.ui.IVerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.ui.TabsManager
import com.jetpackduba.gitnuro.ui.VerticalSplitPaneConfig
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.updates.UpdatesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import java.awt.Desktop
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
class TabViewModel @Inject constructor(
    private val initLocalRepositoryUseCase: InitLocalRepositoryUseCase,
    private val openRepositoryUseCase: OpenRepositoryUseCase,
    private val openSubmoduleRepositoryUseCase: OpenSubmoduleRepositoryUseCase,
    private val tabState: TabState,
    val appStateManager: AppStateManager,
    private val fileChangesWatcher: FileChangesWatcher,
    private val credentialsStateManager: CredentialsStateManager,
    private val openFilePickerUseCase: OpenFilePickerUseCase,
    private val openUrlInBrowserUseCase: OpenUrlInBrowserUseCase,
    private val tabScope: CoroutineScope,
    private val verticalSplitPaneConfig: VerticalSplitPaneConfig,
    val tabViewModelsProvider: TabViewModelsProvider,
    private val globalMenuActionsViewModel: GlobalMenuActionsViewModel,
    private val repositoryOpenViewModelProvider: Provider<RepositoryOpenViewModel>,
    updatesRepository: UpdatesRepository,
) : IVerticalSplitPaneConfig by verticalSplitPaneConfig,
    IGlobalMenuActionsViewModel by globalMenuActionsViewModel {
    var initialPath: String? = null // Stores the path that should be opened when the tab is selected
    val errorsManager: ErrorsManager = tabState.errorsManager
    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem

    val repositoryOpenViewModel: RepositoryOpenViewModel by lazy {
        repositoryOpenViewModelProvider.get()
    }
    private val _repositorySelectionStatus = MutableStateFlow<RepositorySelectionStatus>(RepositorySelectionStatus.None)
    val repositorySelectionStatus: StateFlow<RepositorySelectionStatus>
        get() = _repositorySelectionStatus

    val processing: StateFlow<ProcessingState> = tabState.processing

    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState

    val showError = MutableStateFlow(false)

    init {
        tabScope.run {

            launch {
                errorsManager.error.collect {
                    showError.value = true
                }
            }
        }
    }

    fun openRepository(directory: String) {
        openRepository(File(directory))
    }

    fun openRepository(directory: File) = tabState.safeProcessingWithoutGit {
        printLog(TAG, "Trying to open repository ${directory.absoluteFile}")

        _repositorySelectionStatus.value = RepositorySelectionStatus.Opening(directory.absolutePath)

        try {
            val repository: Repository = if (directory.listFiles()?.any { it.name == ".git" && it.isFile } == true) {
                openSubmoduleRepositoryUseCase(directory)
            } else {
                openRepositoryUseCase(directory)
            }

            repository.workTree // test if repository is valid
            _repositorySelectionStatus.value = RepositorySelectionStatus.Open(repository)
            val git = Git(repository)
            tabState.initGit(git)

            val path = if (directory.name == ".git") {
                directory.parent
            } else
                directory.absolutePath

            onRepositoryChanged(path)
            tabState.newSelectedItem(selectedItem = SelectedItem.UncommittedChanges)
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
        credentialsStateManager.updateState(CredentialsState.CredentialsDenied)
    }

    fun httpCredentialsAccepted(user: String, password: String) {
        credentialsStateManager.updateState(CredentialsAccepted.HttpCredentialsAccepted(user, password))
    }

    fun sshCredentialsAccepted(password: String) {
        credentialsStateManager.updateState(CredentialsAccepted.SshCredentialsAccepted(password))
    }

    var onRepositoryChanged: (path: String?) -> Unit = {}

    fun dispose() {
        fileChangesWatcher.close()
        tabScope.cancel()
    }

    fun openDirectoryPicker(): String? {
        val latestDirectoryOpened = appStateManager.latestOpenedRepositoryPath

        return openFilePickerUseCase(PickerType.DIRECTORIES, latestDirectoryOpened)
    }

    fun initLocalRepository(dir: String) = tabState.safeProcessingWithoutGit {
        val repoDir = File(dir)
        initLocalRepositoryUseCase(repoDir)
        openRepository(repoDir)
    }

    val update: StateFlow<Update?> = updatesRepository.hasUpdatesFlow
        .stateIn(tabScope, started = SharingStarted.Eagerly, null)

    fun gpgCredentialsAccepted(password: String) {
        credentialsStateManager.updateState(CredentialsAccepted.GpgCredentialsAccepted(password))
    }

    fun cancelOngoingTask() {
        tabState.cancelCurrentTask()
    }

    fun openUrlInBrowser(url: String) {
        openUrlInBrowserUseCase(url)
    }

    fun removeRepositoryFromRecent(repository: String) {
        appStateManager.removeRepositoryFromRecent(repository)
    }
}


sealed class RepositorySelectionStatus {
    data object None : RepositorySelectionStatus()
    data class Opening(val path: String) : RepositorySelectionStatus()
    data class Open(val repository: Repository) : RepositorySelectionStatus()
}