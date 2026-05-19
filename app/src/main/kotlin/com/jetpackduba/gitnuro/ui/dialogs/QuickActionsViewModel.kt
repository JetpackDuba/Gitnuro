package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.usecases.GetWorktreeUseCase
import com.jetpackduba.gitnuro.domain.usecases.RefreshAllUseCase
import com.jetpackduba.gitnuro.system.OpenPathInSystemGitAction
import kotlinx.coroutines.launch
import javax.inject.Inject

class QuickActionsViewModel @Inject constructor(
    private val refreshAllUseCase: RefreshAllUseCase,
    private val getWorktreeUseCase: GetWorktreeUseCase,
    private val openPathInSystemGitAction: OpenPathInSystemGitAction,
) : TabViewModel() {

    // TODO Implement bunch of methods

    fun refreshRepository() = refreshAllUseCase()

    fun openProjectInFileExplorer() {
        viewModelScope.launch {
            val worktree = getWorktreeUseCase()

            if (worktree is Either.Ok) {
                openPathInSystemGitAction(worktree.value)
            }
        }
    }
}