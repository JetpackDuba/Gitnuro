package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IStageByDirectoryGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StageByDirectoryGitAction @Inject constructor() : IStageByDirectoryGitAction {
    override suspend operator fun invoke(repositoryPath: String, dir: String) = jgit(repositoryPath) {
        add()
            .addFilepattern(dir)
            .call()

        Unit
    }
}
