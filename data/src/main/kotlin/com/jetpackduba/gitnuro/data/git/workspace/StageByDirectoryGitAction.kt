package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.domain.interfaces.IStageByDirectoryGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StageByDirectoryGitAction @Inject constructor() : IStageByDirectoryGitAction {
    override suspend operator fun invoke(git: Git, dir: String) = withContext(Dispatchers.IO) {
        git.add()
            .addFilepattern(dir)
            .call()
    }
}
