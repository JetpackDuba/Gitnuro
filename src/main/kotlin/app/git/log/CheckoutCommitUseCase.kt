package app.git.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class CheckoutCommitUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, revCommit: RevCommit): Unit = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setName(revCommit.name)
            .call()
    }
}