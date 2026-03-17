package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IMergeBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class BranchMergeUseCase @Inject constructor(
    private val mergeBranchGitAction: IMergeBranchGitAction,
  //  private val appSettingsRepository: AppSettingsRepository,
    private val tabState: TabInstanceRepository,
) {

    fun invoke(branch: Branch) {
        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch merge",
            subtitle = "Merging branch ${branch.simpleName}",
            taskType = TaskType.MERGE_BRANCH,
            refreshEvenIfCrashes = true,
        ) { git ->
            if (mergeBranchGitAction(git, branch, true, true)) { // TODO Fix parameters provided  //appSettingsRepository.ffMerge)) {
                warningNotification("Merge produced conflicts, please fix them to continue.")
            } else {
                positiveNotification("Merged from \"${branch.simpleName}\"")
            }
        }
    }
}