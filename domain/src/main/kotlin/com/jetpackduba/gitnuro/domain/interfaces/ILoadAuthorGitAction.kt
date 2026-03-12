package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import org.eclipse.jgit.api.Git

interface ILoadAuthorGitAction {
    suspend operator fun invoke(git: Git): AuthorInfo
}