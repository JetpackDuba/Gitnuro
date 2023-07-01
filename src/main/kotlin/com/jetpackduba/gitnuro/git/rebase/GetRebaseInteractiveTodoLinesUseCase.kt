package com.jetpackduba.gitnuro.git.rebase

import com.jetpackduba.gitnuro.logging.printDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.lib.RebaseTodoLine
import javax.inject.Inject

private const val TAG = "GetRebaseInteractiveTod"

class GetRebaseInteractiveTodoLinesUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): List<RebaseTodoLine> = withContext(Dispatchers.IO) {
        val repository = git.repository

        val filePath = "${RebaseCommand.REBASE_MERGE}/${RebaseConstants.GIT_REBASE_TODO}"
        val lines = repository.readRebaseTodo(filePath, false)

        printDebug(TAG, "There are ${lines.count()} lines")

        return@withContext lines
    }
}