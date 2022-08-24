package app.git.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

class GetRepositoryStateUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): RepositoryState = withContext(Dispatchers.IO) {
        return@withContext git.repository.repositoryState
    }

}