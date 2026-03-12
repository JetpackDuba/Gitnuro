package com.jetpackduba.gitnuro.data.git.repository

import com.jetpackduba.gitnuro.domain.interfaces.IInitLocalRepositoryGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File
import javax.inject.Inject

private const val INITIAL_BRANCH_NAME = "main"

class InitLocalRepositoryGitAction @Inject constructor() : IInitLocalRepositoryGitAction {
    override suspend operator fun invoke(repoDir: File): Unit = withContext(Dispatchers.IO) {
        Git.init()
            .setInitialBranch(INITIAL_BRANCH_NAME)
            .setDirectory(repoDir)
            .call()
    }
}