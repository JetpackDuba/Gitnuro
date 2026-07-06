package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IRenameBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISetTrackingBranchGitAction
import javax.inject.Inject

class RenameBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val renameBranchGitAction: IRenameBranchGitAction,
    private val setTrackingBranchGitAction: ISetTrackingBranchGitAction,
) {
    suspend operator fun invoke(oldName: String, newName: String): Either<Unit, AppError> {
        return useCaseExecutor.execute(
            dataToRefresh = arrayOf(DataToRefresh.BRANCHES, DataToRefresh.LOG),
        ) { repositoryPath ->
            val branch = renameBranchGitAction(repositoryPath, oldName, newName).bind()

            setTrackingBranchGitAction(repositoryPath, branch, null, null)
        }
    }
}