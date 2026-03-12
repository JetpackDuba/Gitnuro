package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.interfaces.IUnstageAllGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class UnstageAllGitAction @Inject constructor() : IUnstageAllGitAction {
    override suspend operator fun invoke(git: Git, entries: List<StatusEntry>?): Unit = withContext(Dispatchers.IO) {
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