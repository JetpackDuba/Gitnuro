package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitFromHashGitAction
import com.jetpackduba.gitnuro.domain.repositories.DataState
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class GetCommitFromHashUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val getCommitFromHashGitAction: IGetCommitFromHashGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    suspend operator fun invoke(commitHash: String) = useCaseExecutor.execute { repositoryPath ->
        val logDataState = repositoryDataRepository.log.value
        val commit = (logDataState as? DataState.Loaded)?.data[commitHash]?.commit

        if (commit != null) {
            return@execute Either.Ok(commit)
        }

        getCommitFromHashGitAction(repositoryPath, commitHash)
    }
}