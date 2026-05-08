package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IResetToCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class ResetBranchUseCase @Inject constructor(
    private val resetToCommitGitAction: IResetToCommitGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(revCommit: Commit, resetType: ResetType) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ResetToCommit,
            onRefresh = {
                refreshAllUseCase()
            },
        ) { repositoryPath ->
            resetToCommitGitAction(repositoryPath, revCommit, resetType = resetType)
        }
    }
}



enum class ResetType {
    SOFT,
    MIXED,
    HARD,
}