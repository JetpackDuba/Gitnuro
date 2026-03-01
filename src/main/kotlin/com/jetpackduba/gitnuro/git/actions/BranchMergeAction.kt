package com.jetpackduba.gitnuro.git.actions

import com.jetpackduba.gitnuro.git.Action
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.MergeBranchUseCase
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.models.warningNotification
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import javax.inject.Inject

class BranchMergeAction @Inject constructor(
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val tabState: TabState,
) : IAction<Action.BranchMerge> {

    override fun invoke(action: Action.BranchMerge) {
        val ref = action.ref

        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch merge",
            subtitle = "Merging branch ${ref.simpleName}",
            taskType = TaskType.MERGE_BRANCH,
            refreshEvenIfCrashes = true,
        ) { git ->
            if (mergeBranchUseCase(git, ref, appSettingsRepository.ffMerge)) {
                warningNotification("Merge produced conflicts, please fix them to continue.")
            } else {
                positiveNotification("Merged from \"${ref.simpleName}\"")
            }
        }
    }
}