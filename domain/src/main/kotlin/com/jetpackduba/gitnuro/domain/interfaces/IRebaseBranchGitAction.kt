package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

typealias IsMultiStep = Boolean

interface IRebaseBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, branch: Branch): Either<IsMultiStep, GitError>
}