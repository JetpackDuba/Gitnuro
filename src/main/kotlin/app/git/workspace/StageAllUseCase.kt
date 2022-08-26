package app.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class StageAllUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git
            .add()
            .addFilepattern(".")
            .setUpdate(true) // Modified and deleted files
            .call()
        git
            .add()
            .addFilepattern(".")
            .setUpdate(false) // For newly added files
            .call()
    }
}