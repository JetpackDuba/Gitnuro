package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Tag
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetTagsGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<List<Tag>, GitError>
}