package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch

interface IPushBranchGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        force: Boolean,
        pushTags: Boolean,
        pushWithLease: Boolean,
        specificBranch: Branch? = null,
    ): Either<Unit, GitError>
}