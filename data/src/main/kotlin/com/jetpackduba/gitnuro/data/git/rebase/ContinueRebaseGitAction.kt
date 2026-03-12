package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.domain.interfaces.IContinueRebaseGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

class ContinueRebaseGitAction @Inject constructor() : IContinueRebaseGitAction {
    override suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.CONTINUE)
            .call()
    }
}