package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.submodules.*
import com.jetpackduba.gitnuro.ui.TabsManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.submodule.SubmoduleStatus

class SubmodulesViewModel @AssistedInject constructor(
    private val tabState: TabState,
    private val getSubmodulesUseCase: GetSubmodulesUseCase,
    private val initializeSubmoduleUseCase: InitializeSubmoduleUseCase,
    private val updateSubmoduleUseCase: UpdateSubmoduleUseCase,
    private val syncSubmoduleUseCase: SyncSubmoduleUseCase,
    private val deInitializeSubmoduleUseCase: DeInitializeSubmoduleUseCase,
    private val addSubmoduleUseCase: AddSubmoduleUseCase,
    private val deleteSubmoduleUseCase: DeleteSubmoduleUseCase,
    private val tabScope: CoroutineScope,
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
            tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.UNCOMMITTED_CHANGES, RefreshType.SUBMODULES) {
                refresh(tabState.git)
            }
        }
    }

    private suspend fun loadSubmodules(git: Git) {
        _submodules.value = getSubmodulesUseCase(git).toList()
    }

    fun initializeSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.SUBMODULES,
    ) { git ->
        initializeSubmoduleUseCase(git, path)
        updateSubmoduleUseCase(git, path)
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
    ) { git ->
        deInitializeSubmoduleUseCase(git, path)
    }

    fun onSyncSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.SUBMODULES,
        title = "Syncing submodule $path",
        subtitle = "Please wait until synchronization has finished",
    ) { git ->
        syncSubmoduleUseCase(git, path)
    }

    fun onUpdateSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.SUBMODULES,
        title = "Updating submodule $path",
        subtitle = "Please wait until update has finished",
    ) { git ->
        updateSubmoduleUseCase(git, path)
    }

    fun onCreateSubmodule(repository: String, directory: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        addSubmoduleUseCase(
            git = git,
            name = directory,
            path = directory,
            uri = repository,
        )
    }

    fun onDeleteSubmodule(path: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        deleteSubmoduleUseCase(git, path)
    }
}

data class SubmodulesState(val submodules: List<Pair<String, SubmoduleStatus>>, val isExpanded: Boolean)