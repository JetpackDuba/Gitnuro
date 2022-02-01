package app.git

import app.extensions.isBranch
import app.extensions.simpleName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class BranchesManager @Inject constructor() {
    /**
     * Returns the current branch in [Ref]. If the repository is new, the current branch will be null.
     */
    suspend fun currentBranchRef(git: Git): Ref? {
        val branchList = getBranches(git)
        val branchName = git
            .repository
            .fullBranch

        return branchList.firstOrNull { it.name == branchName }
    }

    suspend fun getBranches(git: Git) = withContext(Dispatchers.IO) {
        return@withContext git
            .branchList()
            .call()
    }

    suspend fun createBranch(git: Git, branchName: String) = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setCreateBranch(true)
            .setName(branchName)
            .call()
    }

    suspend fun createBranchOnCommit(git: Git, branch: String, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setCreateBranch(true)
            .setName(branch)
            .setStartPoint(revCommit)
            .call()
    }

    suspend fun deleteBranch(git: Git, branch: Ref) = withContext(Dispatchers.IO) {
        git
            .branchDelete()
            .setBranchNames(branch.name)
            .setForce(true) // TODO Should it be forced?
            .call()
    }

    suspend fun remoteBranches(git: Git) = withContext(Dispatchers.IO) {
        git
            .branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call()
    }

    suspend fun checkoutRef(git: Git, ref: Ref) = withContext(Dispatchers.IO) {
        git.checkout().apply {
            setName(ref.name)
            if (ref.isBranch && ref.name.startsWith("refs/remotes/")) {
                setCreateBranch(true)
                setName(ref.simpleName)
                setStartPoint(ref.objectId.name)
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            }
            call()
        }
    }
}