package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IGetBranchesGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetBranchesGitAction @Inject constructor() : IGetBranchesGitAction {
    // TODO after refactor remove this overload
    override suspend operator fun invoke(git: Git): List<Ref> = withContext(Dispatchers.IO) {
        return@withContext invoke(git.repository.directory.absolutePath)
    }

    override suspend operator fun invoke(repository: String): List<Ref> = withContext(Dispatchers.IO) {
        return@withContext jgit(repository) {
            branchList()
                .call()
        }
    }
}