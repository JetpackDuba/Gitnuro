package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.JGitBranchMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemoteBranchesGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetRemoteBranchesGitAction @Inject constructor(
    private val jGitBranchMapper: JGitBranchMapper,
) : IGetRemoteBranchesGitAction {
    override suspend operator fun invoke(git: Git): List<Branch> = withContext(Dispatchers.IO) {
        git
            .branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call()
            .mapNotNull { jGitBranchMapper.toDomain(it) }
    }
}