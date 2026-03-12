package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IDeInitializeSubmoduleGitAction {
    suspend operator fun invoke(git: Git, path: String): Unit
}