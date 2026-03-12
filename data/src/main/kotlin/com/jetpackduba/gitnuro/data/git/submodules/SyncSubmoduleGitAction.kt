package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.domain.interfaces.ISyncSubmoduleGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class SyncSubmoduleGitAction @Inject constructor() : ISyncSubmoduleGitAction {
    override suspend operator fun invoke(git: Git, path: String): Unit = withContext(Dispatchers.IO) {
        git.submoduleSync()
            .addPath(path)
            .call()
    }
}