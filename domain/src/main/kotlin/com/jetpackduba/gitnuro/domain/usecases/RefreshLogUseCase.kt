package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.errors.either
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetLogGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetStatusGitAction
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INITIAL_COMMITS_LOAD = 2000

class RefreshLogUseCase @Inject constructor(
    private val getLogGitAction: IGetLogGitAction,
    private val getCurrentBranchAction: IGetCurrentBranchGitAction,
    private val getStatusGitAction: IGetStatusGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val tabScope: TabCoroutineScope,
) {
    operator fun invoke() = tabScope.launch {
        val repository = repositoryDataRepository.repositoryPath ?: return@launch

        when (val log = loadLog(repository)) {
            is Either.Err -> logLoadFailed(log.error)
            is Either.Ok -> repositoryDataRepository.updateLog(log.value)
        }
    }

    private fun logLoadFailed(error: AppError) {
        // TODO Do something with the error
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