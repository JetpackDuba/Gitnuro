package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IRebaseBranchGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.warningNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.simpleName
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class BranchRebaseUseCase @Inject constructor(
    private val rebaseBranchGitAction: IRebaseBranchGitAction,
    private val tabState: TabInstanceRepository,
) {

    fun invoke(ref: Ref) {
        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch rebase",
            subtitle = "Rebasing branch ${ref.simpleName}",
            taskType = TaskType.REBASE_BRANCH,
            refreshEvenIfCrashes = true,
        ) { git ->
            if (rebaseBranchGitAction(git, ref)) {
                warningNotification("Rebase produced conflicts, please fix them to continue.")
            } else {
                positiveNotification("\"${ref.simpleName}\" rebased")
            }
        }
    }
}