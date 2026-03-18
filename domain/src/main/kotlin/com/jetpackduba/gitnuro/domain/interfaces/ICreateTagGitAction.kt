package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git

interface ICreateTagGitAction {
    suspend operator fun invoke(git: Git, tag: String, commit: Commit): Unit
}