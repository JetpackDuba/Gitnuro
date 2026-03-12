package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IAddSubmoduleGitAction {
    suspend operator fun invoke(git: Git, name: String, path: String, uri: String): Unit
}