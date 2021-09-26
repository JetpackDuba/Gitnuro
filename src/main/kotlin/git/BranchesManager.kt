package git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

class BranchesManager {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    val branches: StateFlow<List<Ref>>
        get() = _branches

    suspend fun loadBranches(git: Git) = withContext(Dispatchers.IO) {
        val branchList = git
            .branchList()
            .call()

        _branches.value = branchList
    }

    suspend fun createBranch(git: Git, branchName: String) = withContext(Dispatchers.IO) {
        git
            .branchCreate()
            .setName(branchName)
            .call()

        loadBranches(git)
    }

    suspend fun deleteBranch(git: Git, branch: Ref) = withContext(Dispatchers.IO) {
        git
            .branchDelete()
            .setBranchNames(branch.name)
            .call()
    }
}