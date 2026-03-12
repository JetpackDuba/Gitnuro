package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.extensions.hasUntrackedChanges
import com.jetpackduba.gitnuro.domain.interfaces.ICheckHasUncommittedChangesGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class CheckHasUncommittedChangesGitAction @Inject constructor() : ICheckHasUncommittedChangesGitAction {
    override suspend operator fun invoke(git: Git) = withContext(Dispatchers.IO) {
        val status = git
            .status()
            .call()

        return@withContext status.hasUncommittedChanges() || status.hasUntrackedChanges()
    }
}