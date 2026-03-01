package com.jetpackduba.gitnuro.git.actions

import com.jetpackduba.gitnuro.git.Action
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.DeleteBranchUseCase
import com.jetpackduba.gitnuro.models.positiveNotification
import javax.inject.Inject

class BranchDeleteAction @Inject constructor(
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val tabState: TabState,
) : IAction<Action.BranchDelete> {

    override fun invoke(action: Action.BranchDelete) {
        val branch = action.ref

        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch delete",
            subtitle = "Deleting branch ${branch.simpleName}",
            taskType = TaskType.DELETE_BRANCH,
        ) { git ->
            deleteBranchUseCase(git, branch)

            positiveNotification("\"${branch.simpleName}\" deleted")
        }
    }
}