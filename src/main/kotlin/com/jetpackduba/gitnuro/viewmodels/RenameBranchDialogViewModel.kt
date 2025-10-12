package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.RenameBranchUseCase
import com.jetpackduba.gitnuro.git.branches.SetTrackingBranchUseCase
import com.jetpackduba.gitnuro.models.positiveNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RenameBranchDialogViewModel @Inject constructor(
    private val tabState: TabState,
    private val renameBranchUseCase: RenameBranchUseCase,
    private val setTrackingBranchUseCase: SetTrackingBranchUseCase,
) {
    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted = _operationCompleted.asStateFlow()

    fun renameBranch(branch: Ref, newName: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.RENAME_BRANCH,
    ) { git ->

        val newRef = renameBranchUseCase(git, branch.name, newName)

        if (newRef != null) {
            setTrackingBranchUseCase(git, newRef, null, null)
        }
        _operationCompleted.value = true

        positiveNotification("Branch renamed")
    }
}
