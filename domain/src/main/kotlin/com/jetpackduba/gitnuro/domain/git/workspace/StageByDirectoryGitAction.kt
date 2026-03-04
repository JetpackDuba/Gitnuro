package com.jetpackduba.gitnuro.domain.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StageByDirectoryGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git, dir: String) = withContext(Dispatchers.IO) {
        git.add()
            .addFilepattern(dir)
            .call()
    }
}
