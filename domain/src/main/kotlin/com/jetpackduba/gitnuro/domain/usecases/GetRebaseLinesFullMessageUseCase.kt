package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitFromRebaseLineGitAction
import com.jetpackduba.gitnuro.domain.models.RebaseLine
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class GetRebaseLinesFullMessageUseCase @Inject constructor(
    private val getCommitFromRebaseLineGitAction: IGetCommitFromRebaseLineGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(lines: List<RebaseLine>) {
        useCaseExecutor.execute(
            taskType = TaskType.GetLinesForRebaseInteractive
        ) { repositoryPath ->
            val result = lines.associate { line ->
                val commit = getCommitFromRebaseLineGitAction(repositoryPath, line.commit, line.shortMessage).bind()
                val fullMessage = commit?.message ?: line.shortMessage
                line.commit to fullMessage
            }

            Either.Ok(result)
        }
    }
}