package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitFromRebaseLineGitAction
import com.jetpackduba.gitnuro.domain.models.RebaseLine
import javax.inject.Inject

class GetRebaseLinesFullMessageUseCase @Inject constructor(
    private val getCommitFromRebaseLineGitAction: IGetCommitFromRebaseLineGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(lines: List<RebaseLine>): Either<List<RebaseLine>, AppError> {
        return useCaseExecutor.execute { repositoryPath ->
            val result = lines.mapNotNull { line ->
                val commit = getCommitFromRebaseLineGitAction(repositoryPath, line.commit, line.shortMessage).bind() ?: return@mapNotNull null
                val fullMessage = commit.message
                line.copy(commit = commit.hash, fullMessage = fullMessage)
            }

            Either.Ok(result)
        }
    }
}