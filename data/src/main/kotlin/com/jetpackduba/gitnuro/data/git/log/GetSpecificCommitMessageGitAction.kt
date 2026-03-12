package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.domain.interfaces.IGetSpecificCommitMessageGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetSpecificCommitMessageGitAction @Inject constructor(
    private val findCommitGitAction: FindCommitGitAction,
) : IGetSpecificCommitMessageGitAction {
    override suspend operator fun invoke(git: Git, commitId: String): String = withContext(Dispatchers.IO) {
        return@withContext findCommitGitAction(git, commitId)?.fullMessage.orEmpty()
    }
}