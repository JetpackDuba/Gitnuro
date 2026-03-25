package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.mappers.JGitBranchMapper
import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetBranchesGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetBranchesGitAction @Inject constructor(
    private val jGitBranchMapper: JGitBranchMapper,
) : IGetBranchesGitAction {
    // TODO after refactor remove this overload
    override suspend operator fun invoke(git: Git): Either<List<Branch>, AppError> {
        return invoke(git.repository.directory.absolutePath)
    }

    override suspend operator fun invoke(repository: String): Either<List<Branch>, AppError> {
        return jgit(repository) {
            branchList()
                .call()
                .mapNotNull { jGitBranchMapper.toDomain(it) }
        }
    }
}