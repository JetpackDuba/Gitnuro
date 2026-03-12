package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.interfaces.IUnstageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class UnstageEntryGitAction @Inject constructor() : IUnstageEntryGitAction {
    override suspend operator fun invoke(git: Git, statusEntry: StatusEntry): Ref = withContext(Dispatchers.IO) {
        git.reset()
            .addPath(statusEntry.filePath)
            .call()
    }
}