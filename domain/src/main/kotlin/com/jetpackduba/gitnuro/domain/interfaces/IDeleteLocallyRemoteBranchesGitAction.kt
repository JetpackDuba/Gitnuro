package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import org.eclipse.jgit.api.Git

interface IDeleteLocallyRemoteBranchesGitAction {
    suspend operator fun invoke(repositoryPath: String, branches: List<String>): Either<List<String>, GitError>
}