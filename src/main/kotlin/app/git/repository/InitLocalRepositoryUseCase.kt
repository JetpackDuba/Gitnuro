package app.git.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File
import javax.inject.Inject

private const val INITIAL_BRANCH_NAME = "main"

class InitLocalRepositoryUseCase @Inject constructor() {
    suspend operator fun invoke(repoDir: File): Unit = withContext(Dispatchers.IO) {
        Git.init()
            .setInitialBranch(INITIAL_BRANCH_NAME)
            .setDirectory(repoDir)
            .call()
    }
}