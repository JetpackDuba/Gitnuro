package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface ISkipRebaseGitAction {
    suspend operator fun invoke(git: Git): Unit
}