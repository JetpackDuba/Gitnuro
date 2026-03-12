package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.interfaces.IDiscardEntriesGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class DiscardEntriesGitAction @Inject constructor() : IDiscardEntriesGitAction {
    override suspend operator fun invoke(git: Git, statusEntries: List<StatusEntry>, staged: Boolean): Unit {
        // Reset if staged, otherwise only conflicting entries
        val entriesToReset = if (staged) {
            statusEntries.map { it.filePath }
        } else {
            statusEntries
                .filter { statusEntry ->
                    statusEntry.statusType == StatusType.CONFLICTING
                }
                .map { it.filePath }
        }

        if (entriesToReset.isNotEmpty()) {
            resetPaths(git, entriesToReset)
        }

        checkoutPaths(git, statusEntries.map { it.filePath })
    }

    private suspend fun resetPaths(git: Git, paths: List<String>) =
        withContext(Dispatchers.IO) {
            git
                .reset()
                .apply {
                    for (path in paths) {
                        addPath(path)
                    }
                }
                .call()
        }

    private fun checkoutPaths(git: Git, paths: List<String>) {
        git
            .checkout()
            .apply {
                for (path in paths) {
                    addPath(path)
                }
            }
            .call()
    }
}