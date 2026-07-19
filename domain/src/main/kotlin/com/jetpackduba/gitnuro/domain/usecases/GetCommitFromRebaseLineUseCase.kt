package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitFromRebaseLineGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import javax.inject.Inject

class GetCommitFromRebaseLineUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val getCommitFromRebaseGitAction: IGetCommitFromRebaseLineGitAction,
) {
    suspend operator fun invoke(commitShortHash: String, shortMessage: String): Either<Commit?, AppError> {
        return useCaseExecutor.execute { repositoryPath ->
            getCommitFromRebaseGitAction(repositoryPath, commitShortHash, shortMessage)
        }
    }
}