package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IAbortRebaseGitAction
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

class AbortRebaseGitAction @Inject constructor(
    private val jgit: JGit,
) : IAbortRebaseGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git.rebase()
            .setOperation(RebaseCommand.Operation.ABORT)
            .call()

        // TODO check if result is aborted to ensure operation worked?

        Unit
    }
}