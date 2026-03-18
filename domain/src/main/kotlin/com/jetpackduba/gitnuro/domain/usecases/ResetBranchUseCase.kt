package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.extensions.shortName
import com.jetpackduba.gitnuro.domain.interfaces.IResetToCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class ResetBranchUseCase @Inject constructor(
    private val resetToCommitGitAction: IResetToCommitGitAction,
    private val tabState: TabInstanceRepository,
) {
    operator fun invoke(revCommit: Commit, resetType: ResetType) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Branch reset",
        subtitle = "Resetting branch to commit ${revCommit.shortHash}", // TODO Use short name instead of hash when showing progress? More useful..
        taskType = TaskType.RESET_TO_COMMIT,
    ) { git ->
        resetToCommitGitAction(git, revCommit, resetType = resetType)

        positiveNotification("Reset completed")
    }
}



enum class ResetType {
    SOFT,
    MIXED,
    HARD,
}