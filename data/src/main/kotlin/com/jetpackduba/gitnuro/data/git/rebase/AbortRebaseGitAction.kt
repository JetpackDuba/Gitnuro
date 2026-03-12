package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.domain.interfaces.IAbortRebaseGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

class AbortRebaseGitAction @Inject constructor() : IAbortRebaseGitAction {
    override suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.ABORT)
            .call()
    }
}