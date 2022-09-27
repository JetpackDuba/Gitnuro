package com.jetpackduba.gitnuro.git.stash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class PopStashUseCase @Inject constructor(
    private val applyStashUseCase: ApplyStashUseCase,
    private val deleteStashUseCase: DeleteStashUseCase,
) {
    suspend operator fun invoke(git: Git, stash: RevCommit) = withContext(Dispatchers.IO) {
        applyStashUseCase(git, stash)
        deleteStashUseCase(git, stash)
    }
}