package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IDeleteRemoteGitAction {
    suspend operator fun invoke(git: Git, remoteName: String): Unit
}