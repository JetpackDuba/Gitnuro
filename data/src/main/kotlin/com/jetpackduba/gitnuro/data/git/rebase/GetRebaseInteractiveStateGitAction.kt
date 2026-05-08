package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseInteractiveStateGitAction
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import java.io.File
import javax.inject.Inject

class GetRebaseInteractiveStateGitAction @Inject constructor(
    private val getRebaseAmendCommitIdGitAction: GetRebaseAmendCommitIdGitAction,
    private val jgit: JGit,
) : IGetRebaseInteractiveStateGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val repository = git.repository

        val rebaseMergeDir = File(repository.directory, RebaseConstants.REBASE_MERGE)
        val doneFile = File(rebaseMergeDir, RebaseConstants.DONE)
        val stoppedShaFile = File(rebaseMergeDir, RebaseConstants.STOPPED_SHA)

        when {
            !rebaseMergeDir.exists() -> RebaseInteractiveState.None
            doneFile.exists() || stoppedShaFile.exists() -> {
                val commitId: String? = getRebaseAmendCommitIdGitAction(git)

                RebaseInteractiveState.ProcessingCommits(commitId)
            }

            else -> RebaseInteractiveState.AwaitingInteraction
        }
    }
}