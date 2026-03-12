package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import org.eclipse.jgit.api.Git

interface IGetRebaseInteractiveStateGitAction {
    suspend operator fun invoke(git: Git): RebaseInteractiveState
}