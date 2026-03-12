package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseLinesFullMessageGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RebaseTodoLine
import javax.inject.Inject

class GetRebaseLinesFullMessageGitAction @Inject constructor(
    private val getCommitFromRebaseLineGitAction: GetCommitFromRebaseLineGitAction,
) : IGetRebaseLinesFullMessageGitAction {
    override suspend operator fun invoke(
        git: Git,
        rebaseTodoLines: List<RebaseTodoLine>,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        return@withContext rebaseTodoLines.associate { line ->
            val commit = getCommitFromRebaseLineGitAction(git, line.commit, line.shortMessage)
            val fullMessage = commit?.fullMessage ?: line.shortMessage
            line.commit.name() to fullMessage
        }
    }
}