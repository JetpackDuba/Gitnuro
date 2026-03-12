package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.domain.interfaces.IInitializeSubmoduleGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class InitializeSubmoduleGitAction @Inject constructor() : IInitializeSubmoduleGitAction {
    override suspend operator fun invoke(git: Git, path: String): Unit = withContext(Dispatchers.IO) {
        git.submoduleInit()
            .addPath(path)
            .call()
    }
}