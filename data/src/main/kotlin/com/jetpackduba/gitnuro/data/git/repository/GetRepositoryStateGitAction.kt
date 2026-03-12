package com.jetpackduba.gitnuro.data.git.repository

import com.jetpackduba.gitnuro.domain.interfaces.IGetRepositoryStateGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

class GetRepositoryStateGitAction @Inject constructor() : IGetRepositoryStateGitAction {
    override suspend operator fun invoke(git: Git): RepositoryState = withContext(Dispatchers.IO) {
        return@withContext git.repository.repositoryState
    }
}