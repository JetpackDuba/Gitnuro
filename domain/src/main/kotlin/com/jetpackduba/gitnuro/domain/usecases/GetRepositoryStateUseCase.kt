package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.errors.RepositoryPathNotSetError
import com.jetpackduba.gitnuro.domain.interfaces.IGetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.models.RepositoryState
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class GetRepositoryStateUseCase @Inject constructor(
    private val gitRepositoryStateGitAction: IGetRepositoryStateGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    suspend operator fun invoke(): Either<RepositoryState, GitError> {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
        return gitRepositoryStateGitAction(repositoryPath)
    }
}