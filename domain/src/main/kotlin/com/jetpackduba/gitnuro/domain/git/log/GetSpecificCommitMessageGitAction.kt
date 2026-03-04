package com.jetpackduba.gitnuro.domain.git.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetSpecificCommitMessageGitAction @Inject constructor(
    private val findCommitGitAction: FindCommitGitAction,
) {
    suspend operator fun invoke(git: Git, commitId: String): String = withContext(Dispatchers.IO) {
        return@withContext findCommitGitAction(git, commitId)?.fullMessage.orEmpty()
    }
}