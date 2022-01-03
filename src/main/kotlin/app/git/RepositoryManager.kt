package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class RepositoryManager @Inject constructor() {
    suspend fun getRepositoryState(git: Git) = withContext(Dispatchers.IO) {
        return@withContext git.repository.repositoryState
    }
}