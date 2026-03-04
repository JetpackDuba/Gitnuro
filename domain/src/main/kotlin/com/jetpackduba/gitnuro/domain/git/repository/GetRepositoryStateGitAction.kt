package com.jetpackduba.gitnuro.domain.git.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

class GetRepositoryStateGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git): RepositoryState = withContext(Dispatchers.IO) {
        return@withContext git.repository.repositoryState
    }
}