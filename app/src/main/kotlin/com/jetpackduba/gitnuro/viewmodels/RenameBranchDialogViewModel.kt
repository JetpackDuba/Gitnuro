package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.interfaces.IRenameBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RenameBranchDialogViewModel @AssistedInject constructor(
    private val tabState: TabInstanceRepository,
    private val renameBranchGitAction: IRenameBranchGitAction,
    private val setTrackingBranchGitAction: ISetTrackingBranchGitAction,
    @Assisted val branch: Branch,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(branch: Branch): RenameBranchDialogViewModel
    }

    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted = _operationCompleted.asStateFlow()

    fun renameBranch(branch: Branch, newName: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.RenameBranch,
    ) { git ->

        val newRef = renameBranchGitAction(git, branch.name, newName)

        if (newRef != null) {
            setTrackingBranchGitAction(git, newRef, null, null)
        }
        _operationCompleted.value = true

        positiveNotification("Branch renamed")
    }
}
