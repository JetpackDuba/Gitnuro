package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import org.eclipse.jgit.submodule.SubmoduleStatus

interface IGetSubmodulesGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<Map<String, SubmoduleStatus>, GitError>
}