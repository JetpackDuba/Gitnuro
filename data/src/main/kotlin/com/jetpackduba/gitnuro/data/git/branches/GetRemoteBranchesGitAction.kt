package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.data.mappers.JGitBranchMapper
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemoteBranchesGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.ListBranchCommand
import javax.inject.Inject

class GetRemoteBranchesGitAction @Inject constructor(
    private val jGitBranchMapper: JGitBranchMapper,
) : IGetRemoteBranchesGitAction {
    override suspend operator fun invoke(repositoryPath: String): Either<List<Branch>, GitError> {
        return jgit(repositoryPath) {
            branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)
                .call()
                .mapNotNull { jGitBranchMapper.toDomain(it) }
        }
    }
}