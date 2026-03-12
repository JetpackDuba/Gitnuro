package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.domain.interfaces.IDeInitializeSubmoduleGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class DeInitializeSubmoduleGitAction @Inject constructor() : IDeInitializeSubmoduleGitAction {
    override suspend operator fun invoke(git: Git, path: String): Unit = withContext(Dispatchers.IO) {
        git.submoduleDeinit()
            .addPath(path)
            .call()
    }
}