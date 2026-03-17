package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IDeleteBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.simpleName
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class BranchDeleteUseCase @Inject constructor(
    private val deleteBranchGitAction: IDeleteBranchGitAction,
    private val tabState: TabInstanceRepository,
) {

    fun invoke(branch: Branch) {
        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch delete",
            subtitle = "Deleting branch ${branch.simpleName}",
            taskType = TaskType.DELETE_BRANCH,
        ) { git ->
            deleteBranchGitAction(git, branch)

            positiveNotification("\"${branch.simpleName}\" deleted")
        }
    }
}