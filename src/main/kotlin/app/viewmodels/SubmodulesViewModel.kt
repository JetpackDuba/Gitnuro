package app.viewmodels

import app.git.submodules.GetSubmodulesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.submodule.SubmoduleStatus
import javax.inject.Inject

class SubmodulesViewModel @Inject constructor(
    private val getSubmodulesUseCase: GetSubmodulesUseCase
): ExpandableViewModel() {
    private val _submodules = MutableStateFlow<List<Pair<String, SubmoduleStatus>>>(listOf())
    val submodules: StateFlow<List<Pair<String, SubmoduleStatus>>>
        get() = _submodules

    private suspend fun loadSubmodules(git: Git) {
        _submodules.value = getSubmodulesUseCase(git).toList()
    }

    suspend fun refresh(git: Git) {
        loadSubmodules(git)
    }
}