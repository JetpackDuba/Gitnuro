package app.git.stash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class DeleteStashUseCase @Inject constructor(
    private val getStashListUseCase: GetStashListUseCase,
) {
    suspend operator fun invoke(git: Git, stashInfo: RevCommit): Unit = withContext(Dispatchers.IO) {
        val stashList = getStashListUseCase(git)
        val indexOfStashToDelete = stashList.indexOf(stashInfo)

        git.stashDrop()
            .setStashRef(indexOfStashToDelete)
            .call()
    }
}