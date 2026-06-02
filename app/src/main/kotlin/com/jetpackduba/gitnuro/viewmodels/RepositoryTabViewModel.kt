package com.jetpackduba.gitnuro.viewmodels

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.jetpackduba.gitnuro.Screen
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.di.TabComponent
import com.jetpackduba.gitnuro.domain.MAX_COMPLETED_TASKS_KEPT
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.credentials.CredentialsState
import com.jetpackduba.gitnuro.domain.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.domain.interfaces.IFileChangesWatcher
import com.jetpackduba.gitnuro.domain.interfaces.IInitLocalRepositoryGitAction
import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.domain.repositories.*
import com.jetpackduba.gitnuro.domain.usecases.OpenRepositoryUseCase
import com.jetpackduba.gitnuro.domain.usecases.SetRepositorySelectionStateToNoneUseCase
import com.jetpackduba.gitnuro.extensions.stateIn
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.system.OpenFilePickerGitAction
import com.jetpackduba.gitnuro.system.OpenUrlInBrowserGitAction
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.ui.IVerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.VerticalSplitPaneConfig
import com.jetpackduba.gitnuro.ui.components.TabInformationProvider
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.updates.UpdatesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.get
import kotlin.collections.remove
import kotlin.time.Duration.Companion.milliseconds

private const val MIN_TIME_AFTER_GIT_OPERATION = 2000L

private const val TAG = "RepositoryTabViewModel"

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
    private val tabComponent: TabComponent,
    @Assisted val initialPath: String?,
    updatesRepository: UpdatesRepository,
) : IVerticalSplitPaneConfig by verticalSplitPaneConfig,
    IGlobalMenuActionsViewModel by globalMenuActionsViewModel,
    TabViewModel(),
    TabInformationProvider {
    @AssistedFactory
    interface Factory {
        fun create(initialPath: String?): RepositoryTabViewModel
    }

    val savedStates = mutableMapOf<String, Pair<Any?, Any?>>()

    val viewModelsMap = mutableMapOf<NavKey, TabViewModel>()

    fun <T : TabViewModel> getViewModel(key: NavKey, provideVM: (TabComponent) -> T): T {
        if (!viewModelsMap.contains(key)) {
            viewModelsMap[key] = provideVM(tabComponent)
        }

        return viewModelsMap.getValue(key) as T
    }
    fun removeViewModel(key: NavKey) {
        if (!backStack.contains(key)) {
            printLog(TAG, "TAB ${tabName.value} - Removing view model for key $key")
            viewModelsMap[key]?.onClear()
            viewModelsMap.remove(key)
        } else {
            printLog(TAG, "TAB ${tabName.value} - Keeping view model for key $key")
        }
    }


    private val allErrors = repositoryStateRepository
        .completedTasks
        .map { it.filterIsInstance<CompletedTask.Failure>() }
        .distinctUntilChanged()

    private val alreadyDisplayedCompletedTasks = MutableStateFlow<List<CompletedTask>>(emptyList())

    val severeErrors = combine(
        allErrors,
        alreadyDisplayedCompletedTasks.filterIsInstance<List<CompletedTask.Failure>>()
    ) { allErrors, alreadyDisplayedErrors ->
        allErrors - alreadyDisplayedErrors
    }
        .map { it.filter { it.severity == FailureSeverity.HIGH } }
        .distinctUntilChanged()
        .stateIn(emptyList())

    val notifications = combine(
        repositoryStateRepository.completedTasks,
        alreadyDisplayedCompletedTasks,
    ) { tasks, alreadyDisplayedTasks ->
        tasks - alreadyDisplayedTasks
    }
        .map { tasks ->
            tasks.filter { task ->
                when (task) {
                    is CompletedTask.Failure if task.severity == FailureSeverity.HIGH -> false
                    else -> true
                }
            }
        }
        .stateIn(emptyList())

    val repositorySelectionState: StateFlow<RepositorySelectionState> =
        repositoryDataRepository.repositorySelectionState


    val repositoryPath = repositoryDataRepository
        .repositorySelectionState
        .map { state ->
            (state as? RepositorySelectionState.Open)?.path
        }
        .stateIn(null)

    override val tabName: StateFlow<String?> = repositoryDataRepository
        .repositorySelectionState
        .map { state ->
            val path = ((state as? RepositorySelectionState.Open)?.path ?: initialPath)
                ?.removeSuffix(systemSeparator)
                ?.removeSuffix(".git")
                ?.removeSuffix(systemSeparator)


            path?.split(systemSeparator)?.lastOrNull()
        }
        .stateIn(null)

    override val extraInfo: StateFlow<String?> = repositoryPath

    val backStack = NavBackStack<Screen>(
        if (initialPath == null) {
            Screen.Welcome
        } else {
            Screen.RepositoryLoading
        }
    )

    val processingTask = repositoryStateRepository.currentTask
        .debounce(300L.milliseconds)
        .stateIn(initialValue = null)

    val credentialsState: StateFlow<CredentialsState> = credentialsStateManager.credentialsState


    fun openRepository(directory: String) {
        viewModelScope.launch {
            printLog(TAG, "Trying to open repository ${directory}")

            openRepositoryUseCase(directory)
        }
    }

    fun completedTaskAlreadyShown(task: CompletedTask) {
        alreadyDisplayedCompletedTasks.update {
            it
                .toMutableList()
                .apply { add(task) }
                .takeLast(MAX_COMPLETED_TASKS_KEPT)
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

    override fun dispose() {
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
        // TODO Do something at some point?
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
