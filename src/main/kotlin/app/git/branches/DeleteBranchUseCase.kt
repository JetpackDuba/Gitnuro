package app.git.branches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class DeleteBranchUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, branch: Ref): List<String> = withContext(Dispatchers.IO) {
        git
            .branchDelete()
            .setBranchNames(branch.name)
            .setForce(true) // TODO Should it be forced?
            .call()
    }
}