package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IRenameBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, oldName: String, newName: String): Either<Branch, GitError>
}