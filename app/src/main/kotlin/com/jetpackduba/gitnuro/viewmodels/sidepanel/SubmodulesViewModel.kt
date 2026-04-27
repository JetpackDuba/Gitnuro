package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.ui.TabsManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.submodule.SubmoduleStatus

class SubmodulesViewModel @AssistedInject constructor(
    private val tabState: TabInstanceRepository,
    private val getSubmodulesGitAction: IGetSubmodulesGitAction,
    private val initializeSubmoduleGitAction: IInitializeSubmoduleGitAction,
    private val updateSubmoduleGitAction: IUpdateSubmoduleGitAction,
    private val syncSubmoduleGitAction: ISyncSubmoduleGitAction,
    private val deInitializeSubmoduleGitAction: IDeInitializeSubmoduleGitAction,
    private val deleteSubmoduleGitAction: IDeleteSubmoduleGitAction,
    private val tabScope: TabCoroutineScope,
    private val tabsManager: TabsManager,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(true) {

    private val _submodules = MutableStateFlow<List<Pair<String, SubmoduleStatus>>>(listOf())
    val submodules: StateFlow<SubmodulesState> =
        combine(_submodules, isExpanded, filter) { submodules, isExpanded, filter ->
            SubmodulesState(
                submodules = submodules.filter { it.first.lowercaseContains(filter) },
                isExpanded = isExpanded
            )
        }.stateIn(
            scope = tabScope,
            started = SharingStarted.Eagerly,
            initialValue = SubmodulesState(emptyList(), isExpanded.value)
        )

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(
                RefreshType.ALL_DATA,
                RefreshType.UNCOMMITTED_CHANGES,
                RefreshType.SUBMODULES
            ) {
                refresh(tabState.git)
            }
        }
    }

    private suspend fun loadSubmodules(git: Git) {
        _submodules.value = getSubmodulesGitAction(git).toList()
    }

    fun initializeSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.SUBMODULES,
        taskType = TaskType.INIT_SUBMODULE,
    ) { git ->
        initializeSubmoduleGitAction(git, path)
        updateSubmoduleGitAction(git.repository.directory.absolutePath, path)

        null
    }

    suspend fun refresh(git: Git) {
        loadSubmodules(git)
    }

    fun onOpenSubmoduleInTab(path: String) = tabState.runOperation(refreshType = RefreshType.NONE) { git ->
        tabsManager.addNewTabFromPath("${git.repository.workTree}/$path", true)
    }

    fun onDeinitializeSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.SUBMODULES,
        title = "Deinitializing submodule $path",
        taskType = TaskType.DEINIT_SUBMODULE,
    ) { git ->
        deInitializeSubmoduleGitAction(git, path)

        null
    }

    fun onSyncSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.SUBMODULES,
        title = "Syncing submodule $path",
        subtitle = "Please wait until synchronization has finished",
        taskType = TaskType.SYNC_SUBMODULE,
    ) { git ->
        syncSubmoduleGitAction(git, path)

        positiveNotification("Submodule synced")
    }

    fun onUpdateSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.SUBMODULES,
        title = "Updating submodule $path",
        subtitle = "Please wait until update has finished",
        taskType = TaskType.UPDATE_SUBMODULE,
    ) { git ->
        updateSubmoduleGitAction(git.repository.directory.absolutePath, path)

        positiveNotification("Submodule updated")
    }

    fun onDeleteSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.DELETE_SUBMODULE,
    ) { git ->
        deleteSubmoduleGitAction(git, path)

        positiveNotification("Submodule deleted")
    }
}

data class SubmodulesState(val submodules: List<Pair<String, SubmoduleStatus>>, val isExpanded: Boolean)