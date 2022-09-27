package com.jetpackduba.gitnuro.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class DoCommitUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, message: String, amend: Boolean): RevCommit = withContext(Dispatchers.IO) {
        git.commit()
            .setMessage(message)
            .setAllowEmpty(false)
            .setAmend(amend)
            .call()
    }
}