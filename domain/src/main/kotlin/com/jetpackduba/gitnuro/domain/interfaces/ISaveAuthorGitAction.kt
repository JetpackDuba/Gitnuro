package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import org.eclipse.jgit.api.Git

interface ISaveAuthorGitAction {
    suspend operator fun invoke(git: Git, newAuthorInfo: AuthorInfo)
}