package app.git.branches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetRemoteBranchesUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): List<Ref> = withContext(Dispatchers.IO) {
        git
            .branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call()
    }
}