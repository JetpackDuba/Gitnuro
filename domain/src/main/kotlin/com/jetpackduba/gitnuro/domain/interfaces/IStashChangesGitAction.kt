package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IStashChangesGitAction {
    suspend operator fun invoke(git: Git, message: String?): Boolean
}