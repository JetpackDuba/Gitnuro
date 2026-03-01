package com.jetpackduba.gitnuro.git.actions

import com.jetpackduba.gitnuro.git.Action
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.MergeBranchUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseBranchUseCase
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.models.warningNotification
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import javax.inject.Inject

class BranchRebaseAction @Inject constructor(
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val tabState: TabState,
) : IAction<Action.BranchRebase> {

    override fun invoke(action: Action.BranchRebase) {
        val ref = action.ref

        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch rebase",
            subtitle = "Rebasing branch ${ref.simpleName}",
            taskType = TaskType.REBASE_BRANCH,
            refreshEvenIfCrashes = true,
        ) { git ->
            if (rebaseBranchUseCase(git, ref)) {
                warningNotification("Rebase produced conflicts, please fix them to continue.")
            } else {
                positiveNotification("\"${ref.simpleName}\" rebased")
            }
        }
    }
}