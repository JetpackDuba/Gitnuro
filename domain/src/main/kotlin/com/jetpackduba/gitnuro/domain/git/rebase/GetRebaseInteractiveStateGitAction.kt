package com.jetpackduba.gitnuro.domain.git.rebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File
import javax.inject.Inject

class GetRebaseInteractiveStateGitAction @Inject constructor(
    private val getRebaseAmendCommitIdGitAction: GetRebaseAmendCommitIdGitAction,
) {
    suspend operator fun invoke(git: Git): RebaseInteractiveState = withContext(Dispatchers.IO) {
        val repository = git.repository

        val rebaseMergeDir = File(repository.directory, RebaseConstants.REBASE_MERGE)
        val doneFile = File(rebaseMergeDir, RebaseConstants.DONE)
        val stoppedShaFile = File(rebaseMergeDir, RebaseConstants.STOPPED_SHA)

        return@withContext when {
            !rebaseMergeDir.exists() -> RebaseInteractiveState.None
            doneFile.exists() || stoppedShaFile.exists() -> {
                val commitId: String? = getRebaseAmendCommitIdGitAction(git)

                RebaseInteractiveState.ProcessingCommits(commitId)
            }

            else -> RebaseInteractiveState.AwaitingInteraction
        }
    }
}