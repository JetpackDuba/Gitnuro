package com.jetpackduba.gitnuro.domain.git.branches

import com.jetpackduba.gitnuro.domain.git.jgit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetBranchesGitAction @Inject constructor() {
    // TODO after refactor remove this overload
    suspend operator fun invoke(git: Git): List<Ref> = withContext(Dispatchers.IO) {
        return@withContext invoke(git.repository.directory.absolutePath)
    }

    suspend operator fun invoke(repository: String): List<Ref> = withContext(Dispatchers.IO) {
        return@withContext jgit(repository) {
            branchList()
                .call()
        }
    }
}