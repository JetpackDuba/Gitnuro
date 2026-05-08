package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.errors.RepositoryPathNotSetError
import com.jetpackduba.gitnuro.domain.interfaces.IBlameFileGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import org.eclipse.jgit.blame.BlameResult
import javax.inject.Inject

class BlameFileUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val blameFileGitAction: IBlameFileGitAction,
) {
    suspend operator fun invoke(filePath: String): Either<BlameResult, GitError> {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
        return blameFileGitAction(repositoryPath, filePath)
    }
}