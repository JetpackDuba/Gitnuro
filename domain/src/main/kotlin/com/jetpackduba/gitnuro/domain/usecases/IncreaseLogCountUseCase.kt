package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.errors.either
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetLogGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetStatusGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject
import kotlin.math.max

private const val INITIAL_COMMITS_LOAD = 2000

class IncreaseLogCountUseCase @Inject constructor(
    private val getLogGitAction: IGetLogGitAction,
    private val getCurrentBranchAction: IGetCurrentBranchGitAction,
    private val getStatusGitAction: IGetStatusGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(newLimit: Int): Either<Unit, AppError> {
        return useCaseExecutor.execute { repositoryPath ->
            if (newLimit > repositoryDataRepository.maxCommitsToLoadLimit) {
                repositoryDataRepository.maxCommitsToLoadLimit = newLimit
                val log = loadLog(repositoryPath).bind()
                repositoryDataRepository.updateLog(log)
            }

            Either.Ok(Unit)
        }
    }

    private suspend fun loadLog(repository: String) = either {
        val status = getStatusGitAction(repository).bind()
        val currentBranch = getCurrentBranchAction(repository).bind()

        getLogGitAction(
            repository,
            currentBranch,
            hasUncommittedChanges = status.staged.isNotEmpty() || status.unstaged.isNotEmpty(),
            commitsLimit = max(repositoryDataRepository.maxCommitsToLoadLimit, INITIAL_COMMITS_LOAD),
            currentData = repositoryDataRepository.log.value,
            isPaginated = true,
        )
    }
}