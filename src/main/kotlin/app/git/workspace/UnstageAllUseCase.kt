package app.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class UnstageAllUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git
            .reset()
            .call()
    }
}