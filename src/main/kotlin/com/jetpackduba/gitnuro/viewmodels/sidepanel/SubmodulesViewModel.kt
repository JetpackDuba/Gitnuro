package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.submodules.GetSubmodulesUseCase
import com.jetpackduba.gitnuro.git.submodules.InitializeSubmoduleUseCase
import com.jetpackduba.gitnuro.git.submodules.UpdateSubmoduleUseCase
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
    private val tabScope: CoroutineScope,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(true) {

    private val _submodules = MutableStateFlow<List<Pair<String, SubmoduleStatus>>>(listOf())
    val submodules: StateFlow<SubmodulesState> = _submodules.combine(isExpanded) { submodules, isExpanded ->
        SubmodulesState(submodules, isExpanded)
    }.stateIn(
        scope = tabScope,
        started = SharingStarted.Eagerly,
        initialValue = SubmodulesState(emptyList(), isExpanded.value)
    )

    val submodulesState: StateFlow<SubmodulesState> =
        combine(_submodules, isExpanded, filter) { submodules, isExpanded, filter ->
            SubmodulesState(
                submodules = submodules.filter { it.first.lowercaseContains(filter) },
                isExpanded
            )
        }.stateIn(
            tabScope,
            SharingStarted.Eagerly,
            SubmodulesState(emptyList(), isExpanded.value)
        )

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.SUBMODULES)
            {
                refresh(tabState.git)
            }
        }
    }

    private suspend fun loadSubmodules(git: Git) {
        _submodules.value = getSubmodulesUseCase(git).toList()
    }

    fun initializeSubmodule(path: String) = tabState.safeProcessing(
        showError = true,
        refreshType = RefreshType.SUBMODULES,
    ) { git ->
        initializeSubmoduleUseCase(git, path)
        updateSubmoduleUseCase(git, path)
    }

    suspend fun refresh(git: Git) {
        loadSubmodules(git)
    }
}

data class SubmodulesState(val submodules: List<Pair<String, SubmoduleStatus>>, val isExpanded: Boolean)