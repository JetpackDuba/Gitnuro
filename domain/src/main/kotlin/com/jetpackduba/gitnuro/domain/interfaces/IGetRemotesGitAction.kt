package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetRemotesGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<List<Remote>, GitError>
}