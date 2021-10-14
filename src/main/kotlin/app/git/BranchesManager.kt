package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class BranchesManager @Inject constructor() {
    private val _branches = MutableStateFlow<List<Ref>>(listOf())
    val branches: StateFlow<List<Ref>>
        get() = _branches

    private val _currentBranch = MutableStateFlow<String>("")
    val currentBranch: StateFlow<String>
        get() = _currentBranch

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
            .setListMode(ListBranchCommand.ListMode.ALL)
            .call()
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