package app.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import javax.inject.Inject

class GetStatusUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): Status =
        withContext(Dispatchers.IO) {
            git
                .status()
                .call()
        }

}