package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.errors.either
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetLogGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetStatusGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

private const val INITIAL_COMMITS_LOAD = 2000

class RefreshLogUseCase @Inject constructor(
    private val getLogGitAction: IGetLogGitAction,
    private val getCurrentBranchAction: IGetCurrentBranchGitAction,
    private val getStatusGitAction: IGetStatusGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke() {
        useCaseExecutor.executeOnTabScope(TaskType.RefreshLog) { repositoryPath ->
            val log = loadLog(repositoryPath).bind()
            repositoryDataRepository.updateLog(log)
        }
    }

    private suspend fun loadLog(repository: String) = either {
        val status = getStatusGitAction(repository).bind()
        val currentBranch = getCurrentBranchAction(repository).bind()

        getLogGitAction(
            repository,
            currentBranch,
            hasUncommittedChanges = status.staged.isNotEmpty() || status.unstaged.isNotEmpty(),
            commitsLimit = INITIAL_COMMITS_LOAD,
        )
    }
}