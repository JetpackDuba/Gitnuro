package app.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class UnstageEntryUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, statusEntry: StatusEntry): Ref = withContext(Dispatchers.IO) {
        git.reset()
            .addPath(statusEntry.filePath)
            .call()
    }
}