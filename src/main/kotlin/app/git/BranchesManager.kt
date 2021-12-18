package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class BranchesManager @Inject constructor() {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    val branches: StateFlow<List<Ref>>
        get() = _branches

    private val _currentBranch = MutableStateFlow<String>("")
    val currentBranch: StateFlow<String>
        get() = _currentBranch

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

    suspend fun loadBranches(git: Git) = withContext(Dispatchers.IO) {
        val branchList = getBranches(git)

        val branchName = git
            .repository
            .fullBranch

        _branches.value = branchList
        _currentBranch.value = branchName
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

        loadBranches(git)
    }

    suspend fun createBranchOnCommit(git: Git, branch: String, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setCreateBranch(true)
            .setName(branch)
            .setStartPoint(revCommit)
            .call()
    }

    suspend fun mergeBranch(git: Git, branch: Ref, fastForward: Boolean) = withContext(Dispatchers.IO) {
        val fastForwardMode = if (fastForward)
            MergeCommand.FastForwardMode.FF
        else
            MergeCommand.FastForwardMode.NO_FF

        git
            .merge()
            .include(branch)
            .setFastForward(fastForwardMode)
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
}