package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IUpdateSubmoduleGitAction {
    suspend operator fun invoke(git: Git, path: String): Collection<String>
}