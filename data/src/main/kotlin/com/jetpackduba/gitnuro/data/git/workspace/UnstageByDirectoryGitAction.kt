package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageByDirectoryGitAction
import javax.inject.Inject

class UnstageByDirectoryGitAction @Inject constructor() : IUnstageByDirectoryGitAction {
    override suspend operator fun invoke(repositoryPath: String, dir: String): Either<Unit, GitError> = jgit(repositoryPath) {
        reset()
            .addPath(dir)
            .call()
    }
}
