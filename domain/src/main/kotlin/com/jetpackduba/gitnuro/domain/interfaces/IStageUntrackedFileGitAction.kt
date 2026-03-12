package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IStageUntrackedFileGitAction {
    suspend operator fun invoke(git: Git)
}