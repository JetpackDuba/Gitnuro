package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageAllGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import javax.inject.Inject

class UnstageAllGitAction @Inject constructor() : IUnstageAllGitAction {
    override suspend operator fun invoke(repositoryPath: String, entries: List<StatusEntry>?) = jgit(repositoryPath) {
        reset()
            .apply {
                entries?.forEach { entry ->
                    addPath(entry.filePath)
                }
            }
            .call()

        Unit
    }
}