package com.jetpackduba.gitnuro.git.rebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RebaseTodoLine
import javax.inject.Inject

class GetRebaseLinesFullMessageUseCase @Inject constructor(
    private val getCommitFromRebaseLineUseCase: GetCommitFromRebaseLineUseCase,
) {
    suspend operator fun invoke(
        git: Git,
        rebaseTodoLines: List<RebaseTodoLine>,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        return@withContext rebaseTodoLines.associate { line ->
            val commit = getCommitFromRebaseLineUseCase(git, line.commit, line.shortMessage)
            val fullMessage = commit?.fullMessage ?: line.shortMessage
            line.commit.name() to fullMessage
        }
    }
}