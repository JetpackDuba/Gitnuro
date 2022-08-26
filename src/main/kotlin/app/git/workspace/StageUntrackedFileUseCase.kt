package app.git.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class StageUntrackedFileUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git) = withContext(Dispatchers.IO) {
        val diffEntries = git
            .diff()
            .setShowNameAndStatusOnly(true)
            .call()

        val addedEntries = diffEntries.filter { it.changeType == DiffEntry.ChangeType.ADD }

        if (addedEntries.isNotEmpty()) {
            val addCommand = git
                .add()

            for (entry in addedEntries) {
                addCommand.addFilepattern(entry.newPath)
            }

            addCommand.call()
        }
    }
}