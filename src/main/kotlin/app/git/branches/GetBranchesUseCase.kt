package app.git.branches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetBranchesUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): List<Ref> = withContext(Dispatchers.IO) {
        return@withContext git
            .branchList()
            .call()
    }
}