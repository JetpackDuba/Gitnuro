package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.interfaces.IIsBranchMergedGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.revwalk.RevWalk
import javax.inject.Inject

class IsBranchMergedGitAction @Inject constructor(
    private val jgit: JGit,
    private val getBranchesGitAction: GetBranchesGitAction,
    private val getRemoteBranchesGitAction: GetRemoteBranchesGitAction,
) : IIsBranchMergedGitAction {
    override suspend fun invoke(
        repositoryPath: String,
        branch: Branch
    ): Either<Boolean, GitError> = jgit.provide(repositoryPath) { git ->
        val localBranches = getBranchesGitAction(repositoryPath).okOrNull().orEmpty()
        val remoteBranches = getRemoteBranchesGitAction(repositoryPath).okOrNull().orEmpty()
        val allBranches = localBranches + remoteBranches - branch

        RevWalk(git.repository).use { revWalk ->
            val commitOfBranchToDelete = revWalk.parseCommit(git.repository.resolve(branch.name))

            allBranches.any { branch ->
                val otherCommit = revWalk.parseCommit(git.repository.resolve(branch.name))
                revWalk.isMergedInto(commitOfBranchToDelete, otherCommit)
            }

        }
    }
}