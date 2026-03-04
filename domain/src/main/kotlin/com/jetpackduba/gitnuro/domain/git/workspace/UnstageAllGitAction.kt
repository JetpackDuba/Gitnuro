package com.jetpackduba.gitnuro.domain.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class UnstageAllGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git, entries: List<StatusEntry>?): Unit = withContext(Dispatchers.IO) {
        git
            .reset()
            .apply {
                entries?.forEach { entry ->
                    addPath(entry.filePath)
                }
            }
            .call()
    }
}