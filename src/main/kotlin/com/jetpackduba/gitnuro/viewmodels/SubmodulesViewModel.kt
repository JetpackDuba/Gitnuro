package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.submodules.GetSubmodulesUseCase
import com.jetpackduba.gitnuro.git.submodules.InitializeSubmoduleUseCase
import com.jetpackduba.gitnuro.git.submodules.UpdateSubmoduleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.submodule.SubmoduleStatus
import javax.inject.Inject

class SubmodulesViewModel @Inject constructor(
    private val tabState: TabState,
    private val getSubmodulesUseCase: GetSubmodulesUseCase,
    private val initializeSubmoduleUseCase: InitializeSubmoduleUseCase,
    private val updateSubmoduleUseCase: UpdateSubmoduleUseCase,
    private val tabScope: CoroutineScope,
) : ExpandableViewModel() {
    private val _submodules = MutableStateFlow<List<Pair<String, SubmoduleStatus>>>(listOf())
    val submodules: StateFlow<List<Pair<String, SubmoduleStatus>>>
        get() = _submodules

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