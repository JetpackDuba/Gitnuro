package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch

interface IPullFromSpecificBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, remoteBranch: Branch, pullWithRebase: Boolean): Either<PullHasConflicts, GitError>
}