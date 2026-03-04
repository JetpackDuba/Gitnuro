package com.jetpackduba.gitnuro.domain.git.workspace

import com.jetpackduba.gitnuro.domain.extensions.hasUntrackedChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class CheckHasUncommittedChangesGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git) = withContext(Dispatchers.IO) {
        val status = git
            .status()
            .call()

        return@withContext status.hasUncommittedChanges() || status.hasUntrackedChanges()
    }
}