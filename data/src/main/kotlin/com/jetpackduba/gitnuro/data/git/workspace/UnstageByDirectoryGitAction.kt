package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.interfaces.IUnstageByDirectoryGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class UnstageByDirectoryGitAction @Inject constructor() : IUnstageByDirectoryGitAction {
    override suspend operator fun invoke(git: Git, dir: String) = withContext(Dispatchers.IO) {
        git.reset()
            .addPath(dir)
            .call()
    }
}
