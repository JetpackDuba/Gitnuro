package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetCurrentBranchGitAction {
    suspend operator fun invoke(git: Git): Either<Branch?, AppError>

    suspend operator fun invoke(path: String): Either<Branch?, AppError>
}