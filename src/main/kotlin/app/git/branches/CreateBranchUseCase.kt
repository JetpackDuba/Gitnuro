package app.git.branches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class CreateBranchUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, branchName: String): Ref = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setCreateBranch(true)
            .setName(branchName)
            .call()
    }
}