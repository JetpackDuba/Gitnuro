package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.data.git.branches.GetBranchesGitAction
import com.jetpackduba.gitnuro.data.git.branches.GetCurrentBranchGitAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

@TabScope
class BranchesRepository @Inject constructor(
    private val getCurrentBranchGitAction: GetCurrentBranchGitAction,
    private val getBranchesGitAction: GetBranchesGitAction,
) {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    val branches = _branches.asStateFlow()
    private val _currentBranch = MutableStateFlow<Ref?>(null)
    val currentBranch = _currentBranch.asStateFlow()

    suspend fun refresh(git: Git) {
        _currentBranch.value = getCurrentBranchGitAction(git)

        val branchesList = getBranchesGitAction(git).toMutableList()

        // set selected branch as the first one always
        val selectedBranch = branchesList.find { it.name == _currentBranch.value?.name }
        if (selectedBranch != null) {
            branchesList.remove(selectedBranch)
            branchesList.add(0, selectedBranch)
        }

        _branches.value = branchesList
    }
}
