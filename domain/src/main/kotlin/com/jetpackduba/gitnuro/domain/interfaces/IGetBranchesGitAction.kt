package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetBranchesGitAction {
    // TODO after refactor remove this overload
    suspend operator fun invoke(git: Git): Either<List<Branch>, AppError>

    suspend operator fun invoke(repository: String): Either<List<Branch>, AppError>
}