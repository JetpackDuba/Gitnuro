package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.exceptions.UncommittedChangesDetectedException
import com.jetpackduba.gitnuro.domain.interfaces.IRebaseBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.ObjectId
import javax.inject.Inject

class RebaseBranchGitAction @Inject constructor(
    private val jgit: JGit,
) : IRebaseBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, branch: Branch) = jgit.provide(repositoryPath) { git ->
        val rebaseBranch: ObjectId =
            git.repository.resolve(branch.name) ?: throw Exception("Branch ${branch.name} not found")

        val rebaseResult = git.rebase()
            .setOperation(RebaseCommand.Operation.BEGIN)
            .setUpstream(rebaseBranch)
            .call()

        if (rebaseResult.status == RebaseResult.Status.UNCOMMITTED_CHANGES) {
            throw UncommittedChangesDetectedException("Rebase failed, the repository contains uncommitted changes.")
        }

        if (rebaseResult.status == RebaseResult.Status.UNCOMMITTED_CHANGES) {
            throw UncommittedChangesDetectedException("Merge failed, makes sure you repository doesn't contain uncommitted changes.")
        }

        rebaseResult.status == RebaseResult.Status.STOPPED || rebaseResult.status == RebaseResult.Status.CONFLICTS
    }
}