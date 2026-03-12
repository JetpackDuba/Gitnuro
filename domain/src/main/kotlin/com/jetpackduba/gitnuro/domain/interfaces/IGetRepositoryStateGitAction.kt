package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryState

interface IGetRepositoryStateGitAction {
    suspend operator fun invoke(git: Git): RepositoryState
}