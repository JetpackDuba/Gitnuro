package com.jetpackduba.gitnuro.git.rebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

class ContinueRebaseUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.CONTINUE)
            .call()
    }
}