package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.domain.exceptions.UncommittedChangesDetectedException
import com.jetpackduba.gitnuro.domain.interfaces.IRebaseBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IsMultiStep
import com.jetpackduba.gitnuro.domain.models.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RebaseBranchGitAction @Inject constructor() : IRebaseBranchGitAction {
    override suspend operator fun invoke(git: Git, branch: Branch): IsMultiStep = withContext(Dispatchers.IO) {
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

        return@withContext rebaseResult.status == RebaseResult.Status.STOPPED || rebaseResult.status == RebaseResult.Status.CONFLICTS
    }
}