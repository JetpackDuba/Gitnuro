package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit

interface ICreateBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, branchName: String, targetCommit: Commit?): Either<Unit, GitError>
}