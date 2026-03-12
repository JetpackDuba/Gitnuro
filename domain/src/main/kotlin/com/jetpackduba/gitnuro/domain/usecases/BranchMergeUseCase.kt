package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IMergeBranchGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.simpleName
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class BranchMergeUseCase @Inject constructor(
    private val mergeBranchGitAction: IMergeBranchGitAction,
  //  private val appSettingsRepository: AppSettingsRepository,
    private val tabState: TabInstanceRepository,
) {

    fun invoke(ref: Ref) {
        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch merge",
            subtitle = "Merging branch ${ref.simpleName}",
            taskType = TaskType.MERGE_BRANCH,
            refreshEvenIfCrashes = true,
        ) { git ->
            if (mergeBranchGitAction(git, ref, true, true)) { // TODO Fix parameters provided  //appSettingsRepository.ffMerge)) {
                warningNotification("Merge produced conflicts, please fix them to continue.")
            } else {
                positiveNotification("Merged from \"${ref.simpleName}\"")
            }
        }
    }
}