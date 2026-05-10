package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.errors.RepositoryPathNotSetError
import com.jetpackduba.gitnuro.domain.interfaces.ILoadSignOffConfigGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ISaveLocalRepositoryConfigGitAction
import com.jetpackduba.gitnuro.domain.models.SignOffConfig
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class SaveSignOffConfigUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val saveLocalRepositoryConfigGitAction: ISaveLocalRepositoryConfigGitAction,
) {
    suspend operator fun invoke(signOffConfig: SignOffConfig): Either<Unit, GitError> {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
        return saveLocalRepositoryConfigGitAction(repositoryPath, signOffConfig)
    }
}