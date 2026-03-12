package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand

interface IResumeRebaseInteractiveGitAction {
    suspend operator fun invoke(git: Git, interactiveHandler: RebaseCommand.InteractiveHandler)
}